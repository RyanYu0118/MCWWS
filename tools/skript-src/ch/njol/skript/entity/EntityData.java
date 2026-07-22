/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.world.flag.FeatureDependant
 *  io.papermc.paper.world.flag.FeatureFlagSetHolder
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.RegionAccessor
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.entity;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAPIException;
import ch.njol.skript.bukkitutil.EntityUtils;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.entity.SimpleEntityData;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SyntaxElement;
import ch.njol.skript.lang.SyntaxElementInfo;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.localization.Adjective;
import ch.njol.skript.localization.Language;
import ch.njol.skript.localization.LanguageChangeListener;
import ch.njol.skript.localization.Message;
import ch.njol.skript.localization.Noun;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.util.coll.iterator.SingleItemIterator;
import ch.njol.yggdrasil.FieldHandler;
import ch.njol.yggdrasil.Fields;
import ch.njol.yggdrasil.YggdrasilSerializable;
import io.papermc.paper.world.flag.FeatureDependant;
import io.papermc.paper.world.flag.FeatureFlagSetHolder;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.RegionAccessor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EntityData<E extends Entity>
implements SyntaxElement,
YggdrasilSerializable.YggdrasilExtendedSerializable {
    private static final boolean HAS_ENABLED_BY_FEATURE = Skript.methodExists(EntityType.class, "isEnabledByFeature", World.class);
    @Nullable
    private static final Method ENABLED_BY_FEATURE_METHOD;
    @Nullable
    private static final Method IS_ENABLED_METHOD;
    public static final String LANGUAGE_NODE = "entities";
    public static final Message m_age_pattern;
    public static final Adjective m_baby;
    public static final Adjective m_adult;
    private static final List<EntityDataInfo<EntityData<?>>> infos;
    private static final List<EntityData> ALL_ENTITY_DATAS;
    public static Serializer<EntityData> serializer;
    transient EntityDataInfo<?> info;
    protected int codeNameIndex = 0;
    private Kleenean plural = Kleenean.UNKNOWN;
    private Kleenean baby = Kleenean.UNKNOWN;

    public static void onRegistrationStop() {
        infos.forEach(info -> {
            if (SimpleEntityData.class.equals(info.getElementClass())) {
                ALL_ENTITY_DATAS.addAll(Arrays.stream(info.codeNames).map(input -> (EntityData)SkriptParser.parseStatic(input, new SingleItemIterator<EntityDataInfo>((EntityDataInfo)info), null)).collect(Collectors.toList()));
            } else {
                ALL_ENTITY_DATAS.add((EntityData)SkriptParser.parseStatic(info.codeName, new SingleItemIterator<EntityDataInfo>((EntityDataInfo)info), null));
            }
        });
    }

    public static <E extends Entity, T extends EntityData<E>> void register(Class<T> dataClass, String name, Class<E> entityClass, String codeName) throws IllegalArgumentException {
        EntityData.register(dataClass, name, entityClass, 0, codeName);
    }

    public static <E extends Entity, T extends EntityData<E>> void register(Class<T> dataClass, String name, Class<E> entityClass, int defaultName, String ... codeNames) throws IllegalArgumentException {
        EntityType entityType = EntityUtils.toBukkitEntityType(entityClass);
        EntityDataInfo<T> entityDataInfo = new EntityDataInfo<T>(dataClass, name, codeNames, defaultName, entityType, entityClass);
        for (int i = 0; i < infos.size(); ++i) {
            if (!EntityData.infos.get((int)i).entityClass.isAssignableFrom(entityClass)) continue;
            infos.add(i, entityDataInfo);
            return;
        }
        infos.add(entityDataInfo);
    }

    public EntityData() {
        for (EntityDataInfo<EntityData<?>> info : infos) {
            if (this.getClass() != info.getElementClass()) continue;
            this.info = info;
            this.codeNameIndex = info.defaultName;
            return;
        }
        throw new IllegalStateException();
    }

    @Override
    public final boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        Kleenean kleenean = this.plural = parseResult.hasTag("unknown_plural") ? Kleenean.UNKNOWN : Kleenean.get(parseResult.hasTag("plural"));
        this.baby = parseResult.hasTag("baby") ? Kleenean.TRUE : (parseResult.hasTag("adult") ? Kleenean.FALSE : Kleenean.UNKNOWN);
        String codeName = this.info.getCodeNameFromPattern(matchedPattern);
        int matchedCodeName = this.info.getCodeNamePlacement(codeName);
        int patternInCodeName = this.info.getPatternInCodeName(matchedPattern);
        this.codeNameIndex = matchedCodeName;
        return this.init((Literal[])Arrays.copyOf(exprs, exprs.length, Literal[].class), matchedCodeName, patternInCodeName, parseResult);
    }

    protected abstract boolean init(Literal<?>[] var1, int var2, int var3, SkriptParser.ParseResult var4);

    protected abstract boolean init(@Nullable Class<? extends E> var1, @Nullable E var2);

    public abstract void set(E var1);

    protected abstract boolean match(E var1);

    public abstract Class<? extends E> getType();

    @NotNull
    public abstract EntityData<?> getSuperType();

    public final String toString() {
        return this.toString(0);
    }

    protected Noun getName() {
        return this.info.names[this.codeNameIndex];
    }

    @Nullable
    protected Adjective getAgeAdjective() {
        if (this.baby.isTrue()) {
            return m_baby;
        }
        if (this.baby.isFalse()) {
            return m_adult;
        }
        return null;
    }

    public String toString(int flags) {
        Noun name = this.info.names[this.codeNameIndex];
        if (this.baby.isTrue()) {
            return m_baby.toString(name, flags);
        }
        if (this.baby.isFalse()) {
            return m_adult.toString(name, flags);
        }
        return name.toString(flags);
    }

    public Kleenean isPlural() {
        return this.plural;
    }

    public Kleenean isBaby() {
        return this.baby;
    }

    protected abstract int hashCode_i();

    public final int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + this.baby.hashCode();
        result = prime * result + this.plural.hashCode();
        result = prime * result + this.codeNameIndex;
        result = prime * result + this.info.hashCode();
        result = prime * result + this.hashCode_i();
        return result;
    }

    protected abstract boolean equals_i(EntityData<?> var1);

    public final boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EntityData)) {
            return false;
        }
        EntityData other = (EntityData)obj;
        if (this.baby != other.baby) {
            return false;
        }
        if (this.plural != other.plural) {
            return false;
        }
        if (this.codeNameIndex != other.codeNameIndex) {
            return false;
        }
        if (!this.info.equals(other.info)) {
            return false;
        }
        return this.equals_i(other);
    }

    public static EntityDataInfo<?> getInfo(Class<? extends EntityData<?>> entityDataClass) {
        for (EntityDataInfo<EntityData<?>> info : infos) {
            if (info.getElementClass() != entityDataClass) continue;
            return info;
        }
        throw new SkriptAPIException("Unregistered EntityData class " + entityDataClass.getName());
    }

    @Nullable
    public static EntityDataInfo<?> getInfo(String codeName) {
        for (EntityDataInfo<EntityData<?>> info : infos) {
            if (!info.codeName.equals(codeName)) continue;
            return info;
        }
        return null;
    }

    @Nullable
    public static EntityData<?> parse(String string) {
        Iterator<EntityDataInfo<EntityData<?>>> it = new ArrayList(infos).iterator();
        return SkriptParser.parseStatic(Noun.stripIndefiniteArticle(string), it, null);
    }

    @Nullable
    public static EntityData<?> parseWithoutIndefiniteArticle(String string) {
        Iterator<EntityDataInfo<EntityData<?>>> it = new ArrayList(infos).iterator();
        return SkriptParser.parseStatic(string, it, null);
    }

    private E apply(E entity) {
        if (this.baby.isTrue()) {
            EntityUtils.setBaby(entity);
        } else if (this.baby.isFalse()) {
            EntityUtils.setAdult(entity);
        }
        this.set(entity);
        return entity;
    }

    public boolean canSpawn(@Nullable World world) {
        EntityType bukkitEntityType;
        if (world == null) {
            return false;
        }
        EntityType entityType = bukkitEntityType = this.info.entityType != null ? this.info.entityType : EntityUtils.toBukkitEntityType(this);
        if (bukkitEntityType == null || !bukkitEntityType.isSpawnable()) {
            return false;
        }
        if (HAS_ENABLED_BY_FEATURE) {
            assert (ENABLED_BY_FEATURE_METHOD != null);
            try {
                return (Boolean)ENABLED_BY_FEATURE_METHOD.invoke((Object)bukkitEntityType, world);
            }
            catch (IllegalAccessException | InvocationTargetException e) {
                return false;
            }
        }
        assert (IS_ENABLED_METHOD != null);
        try {
            return (Boolean)IS_ENABLED_METHOD.invoke((Object)world, bukkitEntityType);
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    @Nullable
    public final E spawn(Location location) {
        return this.spawn(location, null);
    }

    @Nullable
    public E spawn(Location location, @Nullable Consumer<E> consumer) {
        assert (location != null);
        World world = location.getWorld();
        if (!this.canSpawn(world)) {
            return null;
        }
        if (consumer != null) {
            return (E)EntityData.spawn(location, this.getType(), e -> consumer.accept(this.apply(e)));
        }
        return (E)this.apply(world.spawn(location, this.getType()));
    }

    public E[] getAll(World ... worlds) {
        assert (worlds != null && worlds.length > 0) : Arrays.toString(worlds);
        ArrayList<Entity> list = new ArrayList<Entity>();
        for (World world : worlds) {
            for (Entity entity : world.getEntitiesByClass(this.getType())) {
                if (!this.match(entity)) continue;
                list.add(entity);
            }
        }
        return list.toArray((Entity[])Array.newInstance(this.getType(), list.size()));
    }

    public static <E extends Entity> E[] getAll(EntityData<?>[] types, Class<E> type, World @Nullable [] worlds) {
        assert (types.length > 0);
        if (type == Player.class) {
            if (worlds == null) {
                return (Entity[])Bukkit.getOnlinePlayers().toArray(new Player[0]);
            }
            ArrayList<Player> list = new ArrayList<Player>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!CollectionUtils.contains(worlds, player.getWorld())) continue;
                list.add(player);
            }
            return (Entity[])list.toArray(new Player[list.size()]);
        }
        ArrayList<Entity> list = new ArrayList<Entity>();
        if (worlds == null) {
            worlds = Bukkit.getWorlds().toArray(new World[0]);
        }
        for (World world : worlds) {
            block2: for (Entity entity : world.getEntitiesByClass(type)) {
                for (EntityData<?> entityData : types) {
                    if (!entityData.isInstance(entity)) continue;
                    list.add(entity);
                    continue block2;
                }
            }
        }
        return list.toArray((Entity[])Array.newInstance(type, list.size()));
    }

    public static <E extends Entity> E[] getAll(EntityData<?>[] types, Class<E> type, Chunk[] chunks) {
        assert (types.length > 0);
        ArrayList<Entity> list = new ArrayList<Entity>();
        for (Chunk chunk : chunks) {
            block1: for (Entity entity : chunk.getEntities()) {
                for (EntityData<?> entityData : types) {
                    if (!entityData.isInstance(entity)) continue;
                    list.add(entity);
                    continue block1;
                }
            }
        }
        return list.toArray((Entity[])Array.newInstance(type, list.size()));
    }

    private static <E extends Entity> EntityData<? super E> getData(@Nullable Class<E> entityClass, @Nullable E entity) {
        assert (entityClass == null ^ entity == null);
        assert (entityClass == null || entityClass.isInterface());
        EntityDataInfo<EntityData<?>> closestInfo = null;
        EntityData closestData = null;
        for (EntityDataInfo<EntityData<?>> info : infos) {
            if (info.entityClass == Entity.class || !(entity == null ? info.entityClass.isAssignableFrom(entityClass) : info.entityClass.isInstance(entity))) continue;
            EntityData entityData = null;
            try {
                entityData = (EntityData)info.getElementClass().newInstance();
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (entityData == null || !entityData.init(entityClass, entity) || closestInfo != null && !closestInfo.entityClass.isAssignableFrom(info.entityClass)) continue;
            closestInfo = info;
            closestData = entityData;
        }
        if (closestInfo == null) {
            if (entity != null) {
                return new SimpleEntityData(entity);
            }
            return new SimpleEntityData(entityClass);
        }
        return closestData;
    }

    public static <E extends Entity> EntityData<? super E> fromClass(Class<E> entityClass) {
        return EntityData.getData(entityClass, null);
    }

    public static <E extends Entity> EntityData<? super E> fromEntity(E entity) {
        return EntityData.getData(null, entity);
    }

    public static String toString(Entity entity) {
        return EntityData.fromEntity(entity).getSuperType().toString();
    }

    public static String toString(Class<? extends Entity> entityClass) {
        return EntityData.fromClass(entityClass).getSuperType().toString();
    }

    public static String toString(Entity entity, int flags) {
        return EntityData.fromEntity(entity).getSuperType().toString(flags);
    }

    public static String toString(Class<? extends Entity> entityClass, int flags) {
        return EntityData.fromClass(entityClass).getSuperType().toString(flags);
    }

    public final boolean isInstance(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        if (!this.baby.isUnknown() && EntityUtils.isAgeable(entity) && EntityUtils.isAdult(entity) != this.baby.isFalse()) {
            return false;
        }
        return this.getType().isInstance(entity) && this.match(entity);
    }

    public abstract boolean isSupertypeOf(EntityData<?> var1);

    @Override
    public Fields serialize() throws NotSerializableException {
        return new Fields(this);
    }

    @Override
    public void deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
        fields.setFields(this);
    }

    @Override
    @NotNull
    public String getSyntaxTypeName() {
        return "entity data";
    }

    @Nullable
    protected static <E extends Entity> E spawn(Location location, Class<E> type, Consumer<E> consumer) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        return (E)world.spawn(location, type, consumer);
    }

    @Nullable
    public E create() {
        Location location = ((World)Bukkit.getWorlds().get(0)).getSpawnLocation();
        return this.create(location);
    }

    @Nullable
    public E create(Location location) {
        if (!Skript.methodExists(RegionAccessor.class, "createEntity", new Class[0])) {
            return this.spawn(location);
        }
        return EntityData.create(location, this.getType());
    }

    @Nullable
    protected static <E extends Entity> E create(Location location, Class<E> type) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        return (E)world.createEntity(location, type);
    }

    protected boolean kleeneanMatch(Kleenean from, boolean to) {
        return this.kleeneanMatch(from, Kleenean.get(to));
    }

    protected boolean kleeneanMatch(Kleenean from, Kleenean to) {
        if (from.isUnknown()) {
            return true;
        }
        return from == to;
    }

    protected <T> boolean dataMatch(@Nullable T from, T to) {
        if (from == null) {
            return true;
        }
        return from == to;
    }

    static {
        if (HAS_ENABLED_BY_FEATURE) {
            IS_ENABLED_METHOD = null;
            try {
                ENABLED_BY_FEATURE_METHOD = EntityType.class.getDeclaredMethod("isEnabledByFeature", World.class);
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        ENABLED_BY_FEATURE_METHOD = null;
        try {
            IS_ENABLED_METHOD = FeatureFlagSetHolder.class.getDeclaredMethod("isEnabled", FeatureDependant.class);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        m_age_pattern = new Message("entities.age pattern");
        m_baby = new Adjective("entities.age adjectives.baby");
        m_adult = new Adjective("entities.age adjectives.adult");
        infos = new ArrayList();
        ALL_ENTITY_DATAS = new ArrayList<EntityData>();
        serializer = new Serializer<EntityData>(){

            @Override
            public Fields serialize(EntityData entityData) throws NotSerializableException {
                Fields fields = entityData.serialize();
                fields.putObject("codeName", entityData.info.codeName);
                return fields;
            }

            @Override
            public boolean canBeInstantiated() {
                return false;
            }

            @Override
            public void deserialize(EntityData entityData, Fields fields) throws StreamCorruptedException {
                assert (false);
            }

            @Override
            protected EntityData deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                String codeName = fields.getAndRemoveObject("codeName", String.class);
                if (codeName == null) {
                    throw new StreamCorruptedException();
                }
                EntityDataInfo<?> info = EntityData.getInfo(codeName);
                if (info == null) {
                    throw new StreamCorruptedException("Invalid EntityData code name " + codeName);
                }
                try {
                    EntityData entityData = (EntityData)info.getElementClass().newInstance();
                    entityData.deserialize(fields);
                    return entityData;
                }
                catch (IllegalAccessException | InstantiationException e) {
                    Skript.exception((Throwable)e, new String[0]);
                    throw new StreamCorruptedException();
                }
            }

            @Override
            public boolean mustSyncDeserialization() {
                return false;
            }
        };
        Classes.registerClass(new ClassInfo<EntityData>(EntityData.class, "entitydata").user("entity ?types?").name("Entity Type").description("The type of an <a href='#entity'>entity</a>, e.g. player, wolf, powered creeper, etc.").usage("<i>Detailed usage will be added eventually</i>").examples("victim is a cow", "spawn a creeper").since("1.3").defaultExpression(new SimpleLiteral<SimpleEntityData>(new SimpleEntityData(Entity.class), true)).before("entitytype").supplier(ALL_ENTITY_DATAS::iterator).parser((Parser<SimpleEntityData>)new Parser<EntityData>(){

            @Override
            public String toString(EntityData entityData, int flags) {
                return entityData.toString(flags);
            }

            @Override
            @Nullable
            public EntityData parse(String string, ParseContext context) {
                return EntityData.parse(string);
            }

            @Override
            public String toVariableNameString(EntityData entityData) {
                return "entitydata:" + entityData.toString();
            }
        }).serializer(serializer));
        Variables.yggdrasil.registerFieldHandler(new FieldHandler(){

            @Override
            public boolean excessiveField(Object object, Fields.FieldContext field) throws StreamCorruptedException {
                if (!(object instanceof EntityData)) {
                    return false;
                }
                EntityData entityData = (EntityData)object;
                if (field.getID().equals("matchedPattern")) {
                    entityData.codeNameIndex = (Integer)field.getObject();
                    return true;
                }
                return false;
            }

            @Override
            public boolean missingField(Object object, Field field) throws StreamCorruptedException {
                return object instanceof EntityData;
            }

            @Override
            public boolean incompatibleField(Object object, Field field, Fields.FieldContext context) throws StreamCorruptedException {
                return false;
            }
        });
    }

    private static final class EntityDataInfo<T extends EntityData<?>>
    extends SyntaxElementInfo<T>
    implements LanguageChangeListener {
        final String codeName;
        final String[] codeNames;
        final int defaultName;
        @Nullable
        final EntityType entityType;
        final Class<? extends Entity> entityClass;
        final Noun[] names;
        private String[] patterns;
        private final Map<String, Integer> codeNamePlacements = new HashMap<String, Integer>();
        private final Map<Integer, String> matchedPatternToCodeName = new HashMap<Integer, String>();
        private final Map<Integer, Integer> matchedPatternToCodeNamePattern = new HashMap<Integer, Integer>();

        public EntityDataInfo(Class<T> dataClass, String codeName, String[] codeNames, int defaultName, Class<? extends Entity> entityClass) {
            this(dataClass, codeName, codeNames, defaultName, EntityUtils.toBukkitEntityType(entityClass), entityClass);
        }

        public EntityDataInfo(Class<T> dataClass, String codeName, String[] codeNames, int defaultName, @Nullable EntityType entityType, Class<? extends Entity> entityClass) {
            super(new String[codeNames.length], dataClass, dataClass.getName());
            assert (codeName != null && entityClass != null && codeNames.length > 0);
            this.codeName = codeName;
            this.codeNames = codeNames;
            this.defaultName = defaultName;
            this.entityType = entityType;
            this.entityClass = entityClass;
            this.names = new Noun[codeNames.length];
            for (int i = 0; i < codeNames.length; ++i) {
                assert (codeNames[i] != null);
                this.names[i] = new Noun("entities." + codeNames[i] + ".name");
                this.codeNamePlacements.put(codeNames[i], i);
            }
            Language.addListener(this, Language.LanguageListenerPriority.LATEST);
        }

        @Override
        public void onLanguageChange() {
            ArrayList<String> allPatterns = new ArrayList<String>();
            this.matchedPatternToCodeName.clear();
            this.matchedPatternToCodeNamePattern.clear();
            for (String codeName : this.codeNames) {
                if (Language.keyExistsDefault("entities." + codeName + ".pattern")) {
                    String pattern = Language.get("entities." + codeName + ".pattern").replace("<age>", m_age_pattern.toString());
                    this.matchedPatternToCodeName.put(allPatterns.size(), codeName);
                    this.matchedPatternToCodeNamePattern.put(allPatterns.size(), 0);
                    allPatterns.add(pattern);
                    continue;
                }
                if (!Language.keyExistsDefault("entities." + codeName + ".patterns.0")) {
                    throw new IllegalStateException("lang section for '" + codeName + "' should contain 'pattern' or a 'patterns' section");
                }
                int multiCount = 0;
                while (Language.keyExistsDefault("entities." + codeName + ".patterns." + multiCount)) {
                    String pattern = Language.get("entities." + codeName + ".patterns." + multiCount).replace("<age>", m_age_pattern.toString());
                    this.matchedPatternToCodeName.put(allPatterns.size(), codeName);
                    this.matchedPatternToCodeNamePattern.put(allPatterns.size(), multiCount);
                    allPatterns.add(pattern);
                    ++multiCount;
                }
            }
            this.patterns = (String[])allPatterns.toArray(String[]::new);
        }

        @Override
        public String[] getPatterns() {
            return Arrays.copyOf(this.patterns, this.patterns.length);
        }

        public String getCodeNameFromPattern(int matchedPattern) {
            return this.matchedPatternToCodeName.get(matchedPattern);
        }

        public int getCodeNamePlacement(String codeName) {
            return this.codeNamePlacements.get(codeName);
        }

        public int getPatternInCodeName(int matchedPattern) {
            return this.matchedPatternToCodeNamePattern.get(matchedPattern);
        }

        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof EntityDataInfo)) {
                return false;
            }
            EntityDataInfo other = (EntityDataInfo)obj;
            if (!this.codeName.equals(other.codeName)) {
                return false;
            }
            assert (Arrays.equals(this.codeNames, other.codeNames));
            assert (this.defaultName == other.defaultName);
            assert (this.entityClass == other.entityClass);
            return true;
        }
    }
}

