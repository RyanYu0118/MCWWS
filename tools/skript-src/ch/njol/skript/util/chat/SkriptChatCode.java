/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package ch.njol.skript.util.chat;

import ch.njol.skript.util.chat.ChatCode;
import ch.njol.skript.util.chat.MessageComponent;
import org.jetbrains.annotations.Nullable;

@Deprecated(since="2.15", forRemoval=true)
public enum SkriptChatCode implements ChatCode
{
    reset{

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.reset = true;
        }
    }
    ,
    black("black", '0'),
    dark_blue("dark_blue", '1'),
    dark_green("dark_green", '2'),
    dark_aqua("dark_aqua", "dark_cyan", '3'),
    dark_red("dark_red", '4'),
    dark_purple("dark_purple", '5'),
    gold("gold", "orange", '6'),
    gray("gray", "light_grey", '7'),
    dark_gray("dark_gray", "dark_grey", '8'),
    blue("blue", "light_cyan", '9'),
    green("green", "light_green", 'a'),
    aqua("aqua", "light_cyan", 'b'),
    red("red", "light_red", 'c'),
    light_purple("light_purple", 'd'),
    yellow("yellow", 'e'),
    white("white", 'f'),
    bold{

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.bold = true;
        }
    }
    ,
    italic{

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.italic = true;
        }
    }
    ,
    underlined(null, "underline"){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.underlined = true;
        }
    }
    ,
    strikethrough{

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.strikethrough = true;
        }
    }
    ,
    obfuscated(null, "magic"){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.obfuscated = true;
        }
    }
    ,
    open_url(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            MessageComponent.ClickEvent e;
            component.clickEvent = e = new MessageComponent.ClickEvent(MessageComponent.ClickEvent.Action.open_url, param);
        }
    }
    ,
    run_command(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            MessageComponent.ClickEvent e;
            component.clickEvent = e = new MessageComponent.ClickEvent(MessageComponent.ClickEvent.Action.run_command, param);
        }
    }
    ,
    suggest_command(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            MessageComponent.ClickEvent e;
            component.clickEvent = e = new MessageComponent.ClickEvent(MessageComponent.ClickEvent.Action.suggest_command, param);
        }
    }
    ,
    change_page(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            MessageComponent.ClickEvent e;
            component.clickEvent = e = new MessageComponent.ClickEvent(MessageComponent.ClickEvent.Action.change_page, param);
        }
    }
    ,
    copy_to_clipboard(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.clickEvent = new MessageComponent.ClickEvent(MessageComponent.ClickEvent.Action.copy_to_clipboard, param);
        }
    }
    ,
    show_text(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            MessageComponent.HoverEvent e;
            component.hoverEvent = e = new MessageComponent.HoverEvent(MessageComponent.HoverEvent.Action.show_text, param);
        }
    }
    ,
    font(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.font = param;
        }
    }
    ,
    insertion(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.insertion = param;
        }
    }
    ,
    translate(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.translation = param;
        }
    }
    ,
    keybind(true){

        @Override
        public void updateComponent(MessageComponent component, String param) {
            component.keybind = param;
        }
    };

    private boolean hasParam;
    @Nullable
    private String colorCode;
    @Nullable
    private String langName;
    private char colorChar;

    private SkriptChatCode(String colorCode, String langName, char colorChar) {
        this.colorCode = colorCode;
        this.langName = langName;
        this.hasParam = false;
        this.colorChar = colorChar;
    }

    private SkriptChatCode(String colorCode, String langName) {
        this.colorCode = colorCode;
        this.langName = langName;
        this.hasParam = false;
    }

    private SkriptChatCode(String colorCode, char colorChar) {
        this.colorCode = colorCode;
        this.langName = colorCode;
        this.hasParam = false;
        this.colorChar = colorChar;
    }

    private SkriptChatCode(boolean hasParam) {
        this.hasParam = hasParam;
        this.langName = this.name();
    }

    private SkriptChatCode() {
        this(false);
    }

    @Override
    public boolean hasParam() {
        return this.hasParam;
    }

    @Override
    @Nullable
    public String getColorCode() {
        return this.colorCode;
    }

    @Override
    @Nullable
    public String getLangName() {
        return this.langName;
    }

    @Override
    public boolean isLocalized() {
        return true;
    }

    @Override
    public char getColorChar() {
        return this.colorChar;
    }

    @Override
    public void updateComponent(MessageComponent component, String param) {
    }
}

