package com.maghert.examcommon.thread;


import com.maghert.examcommon.entity.User;

public class UserThreadLocalUtils {

    private static final ThreadLocal<User> userThreadLocal = new ThreadLocal<User>();

    public static User  getUser() {
        return userThreadLocal.get();
    }

    public static void setUser(User user) {
        user.setUser(user);
    }

    public static void clear(){
        userThreadLocal.remove();
    }


}
