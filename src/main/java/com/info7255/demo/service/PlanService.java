package com.info7255.demo.service;


import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

import java.util.*;


@Service
public class PlanService {
    private final Jedis jedis;
    private final ETagService eTagService;

    public PlanService(Jedis jedis, ETagService eTagService) {
        this.jedis = jedis;
        this.eTagService = eTagService;
    }

    public boolean isKeyPresent(String key) {
        Map<String, String> value = jedis.hgetAll(key);
        jedis.close();
        return !(value == null || value.isEmpty());
    }

    public String getETag(String key) {
        return jedis.hget(key, "eTag");
    }

    public String setETag(String key, JSONObject jsonObject) {
        String eTag = eTagService.getETag(jsonObject);
        jedis.hset(key, "eTag", eTag);
        return eTag;
    }

    public String createPlan(JSONObject plan, String key) {
        jsonToMap(plan);
        return setETag(key, plan);
    }

    public Map<String, Object> getPlan(String key) {
        Map<String, Object> result = new HashMap<>();
        getOrDelete(key, result, false);
        return result;
    }

    public void deletePlan(String key) {
        jedis.del(key);
        jedis.close();
    }

    public Map<String, Map<String, Object>> jsonToMap(JSONObject jsonObject) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        Map<String, Object> contentMap = new HashMap<>();

        for (String key : jsonObject.keySet()){
            String redisKey = jsonObject.get("objectType") + ":" + jsonObject.get("objectId");
            Object value = jsonObject.get(key);

            if (value instanceof JSONObject) {
                value = jsonToMap((JSONObject) value);
                jedis.sadd(redisKey + ":" + key, ((Map<String, Map<String, Object>>) value).entrySet().iterator().next().getKey());
            } else if (value instanceof JSONArray) {
                value = jsonToList((JSONArray) value);
                ((List<Map<String, Map<String, Object>>>) value)
                        .forEach((entry) -> {
                            entry.keySet()
                                    .forEach((listKey) -> {
                                        jedis.sadd(redisKey + ":" + key, listKey);
                                        System.out.println("In list, " + redisKey + ":" + key + " -> " + listKey);
                                    });
                        });
            } else {
                jedis.hset(redisKey, key, value.toString());
                contentMap.put(key, value);
                map.put(redisKey, contentMap);
            }
        }
        System.out.println("Map: " + map.toString());
        return map;
    }

    private Map<String, Object> getOrDelete(String redisKey, Map<String, Object> resultMap, boolean isDelete) {
        Set<String> keys = jedis.keys(redisKey + ":*");
        keys.add(redisKey);
        System.out.println("Keys for "+redisKey+": " + keys);
        for (String key: keys) {
            if(key.equals(redisKey)) {
                if(isDelete) jedis.del(new String[]{key});
                else {
                    Map<String, String> object = jedis.hgetAll(key);
                    for (String attrKey : object.keySet()){
                        if (!attrKey.equalsIgnoreCase("eTag")) {
                            resultMap.put(attrKey, isInteger(object.get(attrKey)) ? Integer.parseInt(object.get(attrKey)) : object.get(attrKey));
                        }
                    }
                }
            } else {
                String newKey = key.substring((redisKey + ":").length());
                System.out.println("Key to be serched :" + key + "--------------" + newKey);
                Set<String> members = jedis.smembers(key);
                System.out.println("Members for " + key + ": " + members );
                if (members.size() > 1) {
                    List<Object> listObj = new ArrayList<>();
                    for (String member : members) {
                        if (isDelete) {
                            getOrDelete(member, null, true);
                        } else {
                            Map<String, Object> listMap = new HashMap<>();
                            listObj.add(getOrDelete(member, listMap, false));
                        }
                    }
                    if (isDelete) jedis.del(new String[]{key});
                    else resultMap.put(newKey, listObj);
                } else {
                    if (isDelete) {
                        jedis.del(new String[]{members.iterator().next(), key});
                    } else {
                        Map<String, String> object = jedis.hgetAll(members.iterator().next());
                        System.out.println("All keys : " + object);
                        Map<String, Object> nestedMap = new HashMap<>();
                        for (String attrKey : object.keySet()) {
                            nestedMap.put(attrKey,
                                    isInteger(object.get(attrKey)) ? Integer.parseInt(object.get(attrKey)) : object.get(attrKey));
                        }
                        resultMap.put(newKey, nestedMap);
                    }
                }
            }
        }
        return resultMap;
    }

    public List<Object> jsonToList(JSONArray jsonArray) {
        List<Object> result = new ArrayList<>();
        for (Object value : jsonArray) {
            if (value instanceof JSONArray) value = jsonToList((JSONArray) value);
            else if (value instanceof JSONObject) value = jsonToMap((JSONObject) value);
            result.add(value);
        }
        return result;
    }

    private boolean isInteger(String str){
        try {
            Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
