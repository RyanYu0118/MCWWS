/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  ch.njol.skript.classes.ClassInfo
 *  ch.njol.skript.classes.Parser
 *  ch.njol.skript.classes.Serializer
 *  ch.njol.skript.lang.ParseContext
 *  ch.njol.skript.registrations.Classes
 *  ch.njol.yggdrasil.Fields
 *  org.bukkit.event.Event
 *  org.skriptlang.skript.lang.converter.Converters
 *  org.skriptlang.skript.lang.script.Script
 */
package com.btk5h.skriptmirror.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.yggdrasil.Fields;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.Null;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.skript.reflect.sections.Section;
import com.btk5h.skriptmirror.util.JavaUtil;
import com.btk5h.skriptmirror.util.SkriptUtil;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import org.bukkit.event.Event;
import org.skriptlang.reflect.java.elements.structures.StructImport;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.script.Script;

public class Types {
    static {
        Classes.registerClass((ClassInfo)new ClassInfo(Event.class, "event").user(new String[]{"events?"}).parser((Parser)new Parser<Event>(){

            public Event parse(String s, ParseContext parseContext) {
                return null;
            }

            public boolean canParse(ParseContext context) {
                return false;
            }

            public String toString(Event e, int i) {
                return e.getEventName();
            }

            public String toVariableNameString(Event e) {
                return e.toString();
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
        Classes.registerClass((ClassInfo)new ClassInfo(JavaType.class, "javatype").user(new String[]{"javatypes?"}).parser((Parser)new Parser<JavaType>(){

            public JavaType parse(String s, ParseContext context) {
                Script script = SkriptUtil.getCurrentScript();
                return StructImport.lookup(script, s);
            }

            public boolean canParse(ParseContext context) {
                return true;
            }

            public String toString(JavaType o, int flags) {
                return o.getJavaClass().getName();
            }

            public String toVariableNameString(JavaType o) {
                return "type:" + o.getJavaClass().getName();
            }
        }).serializer((Serializer)new Serializer<JavaType>(){

            public Fields serialize(JavaType cls) {
                Fields f = new Fields();
                f.putObject("type", (Object)cls.getJavaClass().getName());
                return f;
            }

            public void deserialize(JavaType o, Fields f) {
            }

            protected JavaType deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                try {
                    return new JavaType(LibraryLoader.getClassLoader().loadClass((String)fields.getObject("type")));
                }
                catch (ClassNotFoundException e) {
                    throw new NotSerializableException();
                }
            }

            public boolean mustSyncDeserialization() {
                return false;
            }

            public boolean canBeInstantiated(Class<? extends JavaType> aClass) {
                return false;
            }

            protected boolean canBeInstantiated() {
                return false;
            }
        }));
        Converters.registerConverter(ClassInfo.class, JavaType.class, c -> new JavaType(c.getC()));
        Classes.registerClass((ClassInfo)new ClassInfo(Null.class, "null").parser((Parser)new Parser<Null>(){

            public Null parse(String s, ParseContext context) {
                return null;
            }

            public boolean canParse(ParseContext context) {
                return false;
            }

            public String toString(Null o, int flags) {
                return "null";
            }

            public String toVariableNameString(Null o) {
                return "null";
            }

            public String getVariableNamePattern() {
                return "null";
            }
        }).serializer((Serializer)new Serializer<Null>(){

            public Fields serialize(Null o) {
                return new Fields();
            }

            public void deserialize(Null o, Fields f) {
            }

            protected Null deserialize(Fields fields) {
                return Null.getInstance();
            }

            public boolean mustSyncDeserialization() {
                return false;
            }

            public boolean canBeInstantiated(Class<? extends Null> c) {
                return false;
            }

            protected boolean canBeInstantiated() {
                return false;
            }
        }));
        Classes.registerClass((ClassInfo)new ClassInfo(ObjectWrapper.class, "javaobject").user(new String[]{"javaobjects?"}).parser((Parser)new Parser<ObjectWrapper>(){

            public ObjectWrapper parse(String s, ParseContext context) {
                return null;
            }

            public boolean canParse(ParseContext context) {
                return false;
            }

            public String toString(ObjectWrapper objectWrapper, int flags) {
                if (objectWrapper.isArray()) {
                    return JavaUtil.arrayToString(objectWrapper.get(), Classes::toString);
                }
                return Classes.toString((Object)objectWrapper.get());
            }

            public String toVariableNameString(ObjectWrapper o) {
                return o.toString();
            }

            public String getVariableNamePattern() {
                return ".+";
            }
        }));
        Classes.registerClass((ClassInfo)new ClassInfo(Section.class, "section").user(new String[]{"sections?"}));
    }
}

