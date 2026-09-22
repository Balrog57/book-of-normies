package com.normies.book.permission;

import com.normies.book.config.ModConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

public final class PermissionService {
    private static boolean resolved;
    private static Object userManager;
    private static Method getUser;
    private static Method getPrimaryGroup;
    private static Method getInheritedGroups;
    private static Method getQueryOptions;
    private static Method groupGetName;

    private PermissionService() {}

    public static boolean canUse(CommandSourceStack source) {
        if (source.hasPermission(ModConfig.get().permission_level)) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        return player != null && matchesAllowedRole(player.getUUID());
    }

    private static boolean matchesAllowedRole(UUID uuid) {
        if (!initLuckPerms()) {
            return false;
        }
        try {
            Object user = getUser.invoke(userManager, uuid);
            if (user == null) {
                return false;
            }

            Object primary = getPrimaryGroup.invoke(user);
            if (primary != null && isAllowed(primary.toString())) {
                return true;
            }

            Object queryOptions = getQueryOptions.invoke(user);
            @SuppressWarnings("unchecked")
            Collection<Object> groups = (Collection<Object>) getInheritedGroups.invoke(user, queryOptions);
            if (groups == null) {
                return false;
            }
            for (Object group : groups) {
                Object name = groupGetName.invoke(group);
                if (name != null && isAllowed(name.toString())) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
        return false;
    }

    private static boolean isAllowed(String groupName) {
        String needle = groupName.toLowerCase(Locale.ROOT);
        for (String role : ModConfig.get().allowed_roles) {
            if (role != null && role.toLowerCase(Locale.ROOT).equals(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean initLuckPerms() {
        if (resolved) {
            return userManager != null;
        }
        resolved = true;
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = provider.getMethod("get").invoke(null);
            userManager = api.getClass().getMethod("getUserManager").invoke(api);
            getUser = userManager.getClass().getMethod("getUser", UUID.class);

            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
            getPrimaryGroup = userClass.getMethod("getPrimaryGroup");
            getQueryOptions = userClass.getMethod("getQueryOptions");
            getInheritedGroups = userClass.getMethod("getInheritedGroups",
                    Class.forName("net.luckperms.api.query.QueryOptions"));
            groupGetName = Class.forName("net.luckperms.api.model.group.Group").getMethod("getName");
            return true;
        } catch (Throwable t) {
            userManager = null;
            return false;
        }
    }
}
