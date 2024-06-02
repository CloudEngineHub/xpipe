package io.xpipe.core.store;

public interface SingletonSessionStore<T extends Session>
        extends ExpandedLifecycleStore, InternalCacheDataStore, SessionListener {

    @Override
    default void finalizeValidate() throws Exception {
        stopSessionIfNeeded();
    }

    default void setSessionEnabled(boolean value) {
        setCache("sessionEnabled", value);
    }

    default boolean isSessionRunning() {
        return getCache("sessionRunning", Boolean.class, false);
    }

    default boolean isSessionEnabled() {
        return getCache("sessionEnabled", Boolean.class, false);
    }

    @Override
    default void onStateChange(boolean running) {
        setSessionEnabled(running);
        setCache("sessionRunning", running);
    }

    T newSession() throws Exception;

    Class<?> getSessionClass();

    @SuppressWarnings("unchecked")
    default T getSession() {
        return (T) getCache("session", getSessionClass(), null);
    }

    default void startSessionIfNeeded() throws Exception {
        synchronized (this) {
            var s = getSession();
            setSessionEnabled(true);
            if (s != null) {
                if (s.isRunning()) {
                    return;
                }

                s.start();
                return;
            }

            try {
                s = newSession();
                s.start();
                setCache("session", s);
                onStateChange(true);
            } catch (Exception ex) {
                onStateChange(false);
                throw ex;
            }
        }
    }

    default void stopSessionIfNeeded() throws Exception {
        synchronized (this) {
            var ex = getSession();
            setSessionEnabled(false);
            if (ex != null) {
                ex.stop();
                setCache("session", null);
                onStateChange(false);
            }
        }
    }
}
