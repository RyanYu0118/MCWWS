/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.NamespacedKey
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.bukkitutil;

import ch.njol.skript.localization.Message;
import ch.njol.skript.util.ValidationResult;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.log.runtime.RuntimeErrorProducer;

public class NamespacedUtils {
    public static final Message NAMEDSPACED_FORMAT_MESSAGE = new Message("misc.namespacedutils.format");

    public static boolean isValidNamespaceChar(char character) {
        return character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.' || character == '_' || character == '-';
    }

    public static boolean isValidKeyChar(char character) {
        return NamespacedUtils.isValidNamespaceChar(character) || character == '/';
    }

    public static ValidationResult<NamespacedKey> checkValidation(String string) {
        NamespacedKey namespacedKey;
        String key;
        if (string.length() > Short.MAX_VALUE) {
            return new ValidationResult<NamespacedKey>(false, "A namespaced key can not be longer than 32767 characters.");
        }
        String[] split = string.split(":");
        if (split.length > 2) {
            return new ValidationResult<NamespacedKey>(false, "A namespaced key can not have more than one ':'.");
        }
        String string2 = key = split.length == 2 ? split[1] : split[0];
        if (key.isEmpty()) {
            return new ValidationResult<NamespacedKey>(false, "The key cannot be empty.");
        }
        for (char character : key.toCharArray()) {
            if (NamespacedUtils.isValidKeyChar(character)) continue;
            return new ValidationResult<NamespacedKey>(false, "Invalid character '" + character + "'.");
        }
        boolean emptyNamespace = false;
        if (split.length == 2) {
            String namespace = split[0];
            if (!namespace.isEmpty()) {
                for (char character : namespace.toCharArray()) {
                    if (NamespacedUtils.isValidNamespaceChar(character)) continue;
                    return new ValidationResult<NamespacedKey>(false, "Invalid character '" + character + "'.");
                }
                namespacedKey = new NamespacedKey(namespace, key);
            } else {
                emptyNamespace = true;
                namespacedKey = NamespacedKey.minecraft((String)key);
            }
        } else {
            namespacedKey = NamespacedKey.minecraft((String)key);
        }
        if (emptyNamespace) {
            return new ValidationResult<NamespacedKey>(true, "The namespace section of the key is empty. Consider removing the ':'.", namespacedKey);
        }
        return new ValidationResult<NamespacedKey>(true, namespacedKey);
    }

    @Nullable
    public static NamespacedKey checkValidationAndSend(String string, RuntimeErrorProducer producer) {
        ValidationResult<NamespacedKey> validationResult = NamespacedUtils.checkValidation(string);
        String validationMessage = validationResult.message();
        if (!validationResult.valid()) {
            producer.error(validationMessage + ". " + String.valueOf(NAMEDSPACED_FORMAT_MESSAGE));
            return null;
        }
        if (validationMessage != null) {
            producer.warning(validationMessage);
        }
        return validationResult.data();
    }

    public static boolean isValid(String string) {
        return NamespacedUtils.checkValidation(string).valid();
    }
}

