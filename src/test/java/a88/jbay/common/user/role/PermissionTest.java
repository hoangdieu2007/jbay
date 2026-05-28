package a88.jbay.common.user.role;

import a88.jbay.common.network.RequestType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    @Test
    void testAdminHasAllPermissions() {
        assertTrue(Permission.isAllowed(Role.ADMIN, RequestType.BAN));
        assertTrue(Permission.isAllowed(Role.ADMIN, RequestType.BID));
        assertTrue(Permission.isAllowed(Role.ADMIN, RequestType.SELL));
        assertTrue(Permission.isAllowed(Role.ADMIN, RequestType.PING));
        assertTrue(Permission.isAllowed(Role.ADMIN, RequestType.GET_USERS));
        assertTrue(Permission.isAllowed(Role.ADMIN, RequestType.MISC));
    }

    @Test
    void testUserHasBasicPermissions() {
        assertTrue(Permission.isAllowed(Role.USER, RequestType.PING));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.LOGIN));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.LOGOUT));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.BID));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.AUTO_BID));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.CANCEL_AUTO_BID));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.SELL));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.PAY));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.CONFIRM_PAYMENT));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.CANCEL));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.SUBSCRIBE_AUCTION));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.UNSUBSCRIBE_AUCTION));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.GET_AUCTIONS));
        assertTrue(Permission.isAllowed(Role.USER, RequestType.MISC));
    }

    @Test
    void testUserDoesNotHaveAdminPermissions() {
        assertFalse(Permission.isAllowed(Role.USER, RequestType.GET_USERS));
        assertFalse(Permission.isAllowed(Role.USER, RequestType.BAN));
    }

    @Test
    void testBannedUserOnlyHasMisc() {
        assertTrue(Permission.isAllowed(Role.BAN, RequestType.MISC));
        assertFalse(Permission.isAllowed(Role.BAN, RequestType.PING));
        assertFalse(Permission.isAllowed(Role.BAN, RequestType.LOGIN));
        assertFalse(Permission.isAllowed(Role.BAN, RequestType.LOGOUT));
        assertFalse(Permission.isAllowed(Role.BAN, RequestType.BID));
        assertFalse(Permission.isAllowed(Role.BAN, RequestType.SELL));
        assertFalse(Permission.isAllowed(Role.BAN, RequestType.BAN));
        assertFalse(Permission.isAllowed(Role.BAN, RequestType.GET_USERS));
    }
}
