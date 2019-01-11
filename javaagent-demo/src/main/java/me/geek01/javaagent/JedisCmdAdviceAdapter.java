package me.geek01.javaagent;


import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Aspect(className = "redis/clients/jedis/Jedis",
        method = {
                "get",
                "type",
                "append",
                "keys",
                "set",
                "exists",
                "sort",
                "rename",
                "hvals",
                "scan",
                "hexists",
                "hmget",
                "hincrBy",
                "del",
                "randomKey",
                "renamenx",
                "expire",
                "expireAt",
                "move",
                "ttl",
                "mget",
                "getSet",
                "setnx",
                "mset",
                "setex",
                "hlen",
                "hkeys",
                "hdel",
                "zrangeWithScores",
                "zrevrangeWithScores",
                "zrangeByScoreWithScores",
                "zrevrangeByScore",
                "zrevrangeByScoreWithScores",
                "zremrangeByRank",
                "zremrangeByScore",
                "objectRefcount",
                "objectEncoding",
                "objectIdletime",
                "incrBy",
                "decr",
                "incr",
                "decrBy",
                "msetnx",
                "hset",
                "substr",
                "hget",
                "hsetnx",
                "hmset",
                "hgetAll",
                "rpush",
                "lpush",
                "llen",
                "lrange",
                "ltrim",
                "lindex",
                "lset",
                "lrem",
                "lpop",
                "rpop",
                "rpoplpush",
                "sadd",
                "smembers",
                "srem",
                "spop",
                "smove",
                "scard",
                "sismember",
                "sinter",
                "sinterstore",
                "sunion",
                "sunionstore",
                "sdiff",
                "sdiffstore",
                "srandmember",
                "zadd",
                "zrange",
                "zrem",
                "zincrby",
                "zrank",
                "zrevrank",
                "zrevrange",
                "zcard",
                "zscore",
                "watch",
                "blpop",
                "brpop",
                "zcount",
                "zrangeByScore",
                "zunionstore",
                "zinterstore",
                "strlen",
                "lpushx",
                "persist",
                "rpushx",
                "echo",
                "linsert",
                "brpoplpush",
                "setbit",
                "getbit",
                "setrange",
                "getrange",
                "configGet",
                "configSet",
                "eval",
                "subscribe",
                "publish",
                "psubscribe",
                "evalsha",
                "scriptExists",
                "scriptLoad",
                "slowlogGet",
                "bitcount",
                "bitop",
                "dump",
                "restore",
                "pexpire",
                "pexpireAt",
                "pttl",
                "incrByFloat",
                "psetex",
                "clientKill",
                "clientSetname",
                "migrate",
                "hincrByFloat",
                "hscan",
                "sscan",
                "zscan",
                "shutdown",
                "debug",
                "save",
                "sync",
                "select",
                "configResetStat",
                "randomBinaryKey",
                "monitor",
                "unwatch",
                "slowlogReset",
                "slowlogLen",
                "quit",
                "flushDB",
                "dbSize",
                "flushAll",
                "auth",
                "bgsave",
                "bgrewriteaof",
                "lastsave",
                "slaveof",
                "slaveofNoOne",
                "multi",
                "scriptFlush",
                "scriptKill",
                "clientGetname",
                "clientList",
                "slowlogGetBinary",
                "info"
        }
)
public class JedisCmdAdviceAdapter extends BaseAdviceAdapter {
    private String methodName;
    
    public JedisCmdAdviceAdapter(MethodVisitor mv, int acc, String name, String desc,
                                 String appId, AspectInfo aspectInfo) {
        super(mv, acc, name, desc, appId, aspectInfo);
        methodName = name;
    }
    
    @Override
    public void loadOnBeforeArgs() {
        push(appId);
        push(methodName);
    }
    
    @Override
    public void loadOnAfterArgs() {
        loadThis();
        push(methodName);
        push((Type) null);
        loadArgArray();
    }
    
    @Override
    public void loadOnThrowArgs() {
        dup();
        int uncaughtExceptionLocal = newLocal(Type.getType(Throwable.class));
        storeLocal(uncaughtExceptionLocal, Type.getType(Throwable.class));
    
        loadThis();
        push(methodName);
        loadLocal(uncaughtExceptionLocal);
        loadArgArray();
    }
    
    @OnBefore
    @SuppressWarnings("unused")
    public static void injectSpanStart(String appId, String methodName) {
        try {
            Span parentSpan = Tracer.getInstance().getCurrentSpan();
            if (parentSpan == null) {
                return;
            }
            Tracer.getInstance().startSubSpan("Redis " + methodName, appId, Span.SpanType.JEDIS_CMD);
        } catch (Exception e) {
            // ignore
        }
    }
    
    @OnAfter
    @SuppressWarnings("unused")
    public static void injectSpanEnd(Object jedis, String methodName, Object uncaughtException, Object... args) {
        String instance = "";
        String cmd = "";
        try {
            Object client = MethodUtils.invokeExactMethod(jedis, "getClient");
            Integer port = (Integer) MethodUtils.invokeExactMethod(client, "getPort");
            String host = (String) MethodUtils.invokeExactMethod(client, "getHost");
            
            String cmdArgs;
            
            if (args != null) {
                List<String> argsValueList = new ArrayList<>();
                for (Object argsItem : args) {
                    if (argsItem == null ) continue;
                    if (argsItem instanceof String[]) {
                        argsValueList.add(Joiner.on(' ').join((String[])argsItem));
                    } else {
                        argsValueList.add(String.valueOf(argsItem));
                    }
                }
                cmdArgs = Joiner.on(" ").join(argsValueList);
            } else {
                cmdArgs = "";
            }
            cmdArgs = StringUtils.abbreviate(cmdArgs, 200);
            
            instance = host + ":" + port;
            
            cmd = methodName + " " + cmdArgs;
            
            Span parentSpan = Tracer.getInstance().getCurrentSpan();
            if (parentSpan == null) {
                return;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("cmd", cmd);
            map.put("instance", instance);
            Tracer.getInstance().completeSubSpan((Throwable) uncaughtException, null, map);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
        }
    }
}


