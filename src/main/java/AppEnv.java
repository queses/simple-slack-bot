import com.github.shyiko.dotenv.DotEnv;

import java.util.Map;

/**
 * Env storage for application. Built upon at Singleton pattern by `double-checked locking` example.
 * @see <a href="http://www.javenue.info/post/2">Source</a>
 * @see <a href="http://www.javenue.info/post/40">English version</a>
 * Use it as simple as import this class and make a `get("VARIABLE")` call
 */
public final class AppEnv {
    Map<String, String> env;
    private static volatile AppEnv _instance = null;

    private AppEnv() {
        env = DotEnv.load();
    }

    public static synchronized String get(String key) {
        return getEnv().get(key);
    }

    public static synchronized Map<String, String> getEnv() {
        return getInstance().env;
    }

    public static synchronized AppEnv getInstance() {
        if (_instance == null)
            synchronized (AppEnv.class) {
                if (_instance == null) _instance = new AppEnv();
            }
        return _instance;
    }
}
