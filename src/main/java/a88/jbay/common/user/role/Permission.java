package a88.jbay.common.user.role;

import a88.jbay.common.network.RequestType;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Permission {
    private static final Map<Role, Set<RequestType>> rolePermissions = new HashMap<>();

    static {
        //ban role has no permissions
        rolePermissions.put(Role.BAN, EnumSet.of(
                RequestType.MISC
        ));

        //normal auction user
        rolePermissions.put(Role.USER, EnumSet.of(
                RequestType.PING,
                RequestType.LOGIN,
                RequestType.LOGOUT,
                RequestType.BID,
                RequestType.AUTO_BID,
                RequestType.CANCEL_AUTO_BID,
                RequestType.SELL,
                RequestType.PAY,
                RequestType.CONFIRM_PAYMENT,
                RequestType.CANCEL,
                RequestType.SUBSCRIBE_AUCTION,
                RequestType.UNSUBSCRIBE_AUCTION,
                RequestType.GET_AUCTIONS,
                RequestType.MISC
        ));

        //admin
        rolePermissions.put(Role.ADMIN, EnumSet.allOf(RequestType.class));
    }

    public static boolean isAllowed(Role role, RequestType action) {
        return rolePermissions.getOrDefault(role, EnumSet.noneOf(RequestType.class)).contains(action);
    }
}