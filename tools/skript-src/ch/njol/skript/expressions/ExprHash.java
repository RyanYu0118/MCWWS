/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.simplification.SimplifiedLiteral;
import ch.njol.util.Kleenean;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.script.ScriptWarning;

@Name(value="Hash")
@Description(value={"Hashes the given text using the MD5 or SHA algorithms. Each algorithm is suitable for different use cases.", "These hashing algorithms are not suitable for hashing passwords.", "If handling passwords, use a <a href='https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#password-hashing-algorithms'>hashing algorithm specifically designed for passwords</a>.", "MD5 is deprecated and may be removed in a future release. It is provided mostly for backwards compatibility, as it is outdated and not secure. ", "SHA is more secure, but is not suitable for hashing passwords (even with salting). ", "When hashing data, you <strong>must</strong> specify algorithms that will be used for security reasons! ", "Please note that a hash cannot be reversed under normal circumstances. You will not be able to get original value from a hash with Skript."})
@Example(value="set {_hash} to \"hello world\" hashed with SHA-256")
@Since(value={"2.0, 2.2-dev32 (SHA-256 algorithm), 2.12 (SHA-384, SHA-512)"})
public class ExprHash
extends PropertyExpression<String, String> {
    private static final HexFormat HEX_FORMAT = HexFormat.of().withLowerCase();
    private MessageDigest digest;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.setExpr(exprs[0]);
        String algorithm = parseResult.tags.get(0).toUpperCase(Locale.ENGLISH);
        try {
            this.digest = MessageDigest.getInstance(algorithm);
            if (algorithm.equals("MD5") && !this.getParser().getCurrentScript().suppressesWarning(ScriptWarning.DEPRECATED_SYNTAX)) {
                Skript.warning("MD5 is not secure and shouldn't be used if a cryptographically secure hashing algorithm is required.");
            }
            return true;
        }
        catch (NoSuchAlgorithmException e) {
            Skript.error("Unsupported hashing algorithm: " + algorithm);
            return false;
        }
    }

    protected String[] get(Event event, String[] source) {
        String[] result = new String[source.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = HEX_FORMAT.formatHex(this.digest.digest(source[i].getBytes(StandardCharsets.UTF_8)));
        }
        return result;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public Expression<? extends String> simplify() {
        if (this.getExpr() instanceof Literal) {
            return SimplifiedLiteral.fromExpression(this);
        }
        return this;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "hash of " + this.getExpr().toString(event, debug);
    }

    static {
        Skript.registerExpression(ExprHash.class, String.class, ExpressionType.COMBINED, "%strings% hash[ed] with (:(MD5|SHA-256|SHA-384|SHA-512))");
    }
}

