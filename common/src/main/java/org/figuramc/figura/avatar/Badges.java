package org.figuramc.figura.avatar;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.api.sound.SoundAPI;
import org.figuramc.figura.mixin.font.StyleAccessor;
import org.figuramc.figura.permissions.PermissionManager;
import org.figuramc.figura.permissions.Permissions;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;
import org.figuramc.figura.utils.ui.UIHelper;

import java.util.BitSet;
import java.util.Optional;
import java.util.UUID;

public class Badges {

    public static final ResourceLocation FONT = new FiguraIdentifier("badges");

    public static Component fetchBadges(UUID id) {
        if (PermissionManager.get(id).getCategory() == Permissions.Category.BLOCKED)
            return TextComponent.EMPTY.copy();

        // get user data
        Pair<BitSet, BitSet> pair = AvatarManager.getBadges(id);
        if (pair == null)
            return TextComponent.EMPTY.copy();
        Style style = Style.EMPTY.withFont(FONT).withColor(ChatFormatting.WHITE);
        ((StyleAccessor)style).setObfuscated(false);

        MutableComponent badges = TextComponent.EMPTY.copy().withStyle(style);

        // avatar badges
        Avatar avatar = AvatarManager.getAvatarForPlayer(id);
        if (avatar != null) {

            // -- loading -- //

            if (!avatar.loaded)
                badges.append(new TextComponent(Integer.toHexString(Math.abs(FiguraMod.ticks) % 16)));

            // -- mark -- // 

            else if (avatar.nbt != null) {
                // mark
                mark: {
                    // pride (mark skins)
                    BitSet prideSet = pair.getFirst();
                    Pride[] pride = Pride.values();
                    for (int i = pride.length - 1; i >= 0; i--) {
                        if (prideSet.get(i)) {
                            badges.append(pride[i].badge);
                            break mark;
                        }
                    }

                    // mark fallback
                    badges.append(System.DEFAULT.badge.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(ColorUtils.rgbToInt(ColorUtils.userInputHex(avatar.color))))));
                }

                // error
                if (avatar.scriptError) {
                    if (avatar.errorText == null)
                        badges.append(System.ERROR.badge);
                    else
                        badges.append(System.ERROR.badge.copy().withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, System.ERROR.desc.copy().append("\n\n").append(avatar.errorText)))));
                }

                // version
                if (avatar.versionStatus > 0)
                    badges.append(System.WARNING.badge);

                // permissions
                if (!avatar.noPermissions.isEmpty()) {
                    MutableComponent badge = System.PERMISSIONS.badge.copy();
                    MutableComponent desc = System.PERMISSIONS.desc.copy().append("\n");
                    for (Permissions t : avatar.noPermissions)
                        desc.append("\n• ").append(new FiguraText("badges.no_permissions." + t.name.toLowerCase()));

                    badges.append(badge.withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, desc))));
                }
            }
        }

        // -- special -- //
        if (avatar != null) {
            // special badges
            BitSet specialSet = pair.getSecond();
            Special[] specialValues = Special.values();
            for (int i = specialValues.length - 1; i >= 0; i--) {
                if (specialSet.get(i)) {
                    Special special = specialValues[i];
                    Integer color = special.color;
                    if (avatar.badgeToColor.containsKey(special.name().toLowerCase())) {
                        color = ColorUtils.rgbToInt(ColorUtils.userInputHex(avatar.badgeToColor.get(special.name().toLowerCase())));
                    }
                    Component badge = color != null ? special.badge.copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))) : special.badge;
                    badges.append(badge);
                }
            }
        }

        // -- extra -- // 


        // sound
        if (avatar != null && Configs.SOUND_BADGE.value) {
            if (avatar.lastPlayingSound > 0) {
                badges.append(System.SOUND.badge);
            } else if (SoundAPI.getSoundEngine().figura$isPlaying(id)) {
                avatar.lastPlayingSound = 20;
                badges.append(System.SOUND.badge);
            }
        }


        // -- return -- // 
        return badges;
    }

    public static Component noBadges4U(Component text) {
        return TextUtils.replaceInText(text, "[-*/+=❗❌\uD83D\uDEE1★☆❤文✒\uD83D\uDDFF0-9a-f]", TextUtils.UNKNOWN, (s, style) -> style.getFont().equals(FONT) || style.getFont().equals(UIHelper.UI_FONT), Integer.MAX_VALUE);
    }

    public static Pair<BitSet, BitSet> emptyBadges() {
        return Pair.of(new BitSet(Pride.values().length), new BitSet(Special.values().length));
    }

    public static boolean hasCustomBadges(Component text) {
        return text.visit((style, string) -> string.contains("${badges}") || string.contains("${segdab}") ? FormattedText.STOP_ITERATION : Optional.empty(), Style.EMPTY).isPresent();
    }

    public static Component appendBadges(Component text, UUID id, boolean allow) {
        Component badges = allow ? fetchBadges(id) : TextComponent.EMPTY.copy();
        boolean custom = hasCustomBadges(text);

        // no custom badges text
        if (!custom)
            return badges.getString().trim().isEmpty() ? text : text.copy().append(" ").append(badges);

        text = TextUtils.replaceInText(text, "\\$\\{badges\\}(?s)", badges);
        text = TextUtils.replaceInText(text, "\\$\\{segdab\\}(?s)", TextUtils.reverse(badges));

        return text;
    }

    public enum System {
        DEFAULT("△"),
        PERMISSIONS("\uD83D\uDEE1"),
        WARNING("❗"),
        ERROR("❌"),
        SOUND("\uD83D\uDD0A");

        public final Component badge;
        public final Component desc;

        System(String unicode) {
            this.desc = new FiguraText("badges.system." + this.name().toLowerCase());
            this.badge = new TextComponent(unicode).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, desc)));
        }
    }

    public enum Pride {
        AGENDER("ᚠ"),
        AROACE("ᚡ"),
        AROMANTIC("ᚢ"),
        ASEXUAL("ᚣ"),
        BIGENDER("ᚤ"),
        BISEXUAL("ᚥ"),
        DEMIBOY("ᚦ"),
        DEMIGENDER("ᚧ"),
        DEMIGIRL("ᚨ"),
        DEMIROMANTIC("ᚩ"),
        DEMISEXUAL("ᚪ"),
        DISABILITY("ᚫ"),
        FINSEXUAL("ᚬ"),
        GAYMEN("ᚭ"),
        GENDERFAE("ᚮ"),
        GENDERFLUID("ᚯ"),
        GENDERQUEER("ᚰ"),
        INTERSEX("ᚱ"),
        LESBIAN("ᚲ"),
        NONBINARY("ᚳ"),
        PANSEXUAL("ᚴ"),
        PLURAL("ᚵ"),
        POLYSEXUAL("ᚶ"),
        PRIDE("ᚷ"),
        TRANSGENDER("ᚸ");

        public final Component badge;
        public final Component desc;

        Pride(String unicode) {
            this.desc = new FiguraText("badges.pride." + this.name().toLowerCase());
            this.badge = new TextComponent(unicode).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, desc)));
        }
    }

    public enum Special {
        DEV("★"),
        DISCORD_STAFF("☆", ColorUtils.Colors.DISCORD.hex),
        CONTEST("☆", ColorUtils.Colors.AWESOME_BLUE.hex),
        DONATOR("❤", ColorUtils.Colors.AWESOME_BLUE.hex),
        TRANSLATOR("文"),
        TEXTURE_ARTIST("✒"),
        IMMORTALIZED("\uD83D\uDDFF");

        public final Component badge;
        public final Component desc;
        public final Integer color;

        Special(String unicode) {
            this(unicode, null);
        }

        Special(String unicode, Integer color) {
            this.desc = new FiguraText("badges.special." + this.name().toLowerCase());
            Style style = Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, desc));
            if (color != null) style = style.withColor(TextColor.fromRgb(color));
            this.color = color;
            this.badge = new TextComponent(unicode).withStyle(style);
        }
    }
}
