package a88.jbay.model.entity.user.role;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Permission {
    public enum ActionType {
        BID, SELL, BAN, VIEW_AUCTIONS
    }

    private static final Map<Role, Set<ActionType>> rolePermissions = new HashMap<>();

    static {
        //guest role has no permissions
        rolePermissions.put(Role.GUEST, EnumSet.of(
                ActionType.VIEW_AUCTIONS
        ));

        //bidder permissions
        rolePermissions.put(Role.BIDDER, EnumSet.of(
                ActionType.VIEW_AUCTIONS,
                ActionType.BID
        ));

        //seller
        rolePermissions.put(Role.SELLER, EnumSet.of(
                ActionType.VIEW_AUCTIONS,
                ActionType.BID,
                ActionType.SELL
        ));

        //admin
        rolePermissions.put(Role.ADMIN, EnumSet.allOf(ActionType.class));
    }

    public static boolean isAllowed(Role role, ActionType action) {
        return rolePermissions.getOrDefault(role, EnumSet.noneOf(ActionType.class)).contains(action);
    }
}