package a88.jbay.model.entity.user.role;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Permission {
    private static final Map<Role, Set<ActionType>> rolePermissions = new HashMap<>();

    static {
        //guest role has no permissions
        rolePermissions.put(Role.GUEST, EnumSet.of(
                ActionType.VIEW_AUCTIONS
        ));

        //normal auction user
        rolePermissions.put(Role.USER, EnumSet.of(
                ActionType.VIEW_AUCTIONS,
                ActionType.BID,
                ActionType.SELL,
                ActionType.CANCEL
        ));

        //admin
        rolePermissions.put(Role.ADMIN, EnumSet.allOf(ActionType.class));
    }

    public static boolean isAllowed(Role role, ActionType action) {
        return rolePermissions.getOrDefault(role, EnumSet.noneOf(ActionType.class)).contains(action);
    }
}