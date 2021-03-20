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

    public String setETag(String key, JSONObject jsonObject) {
        String eTag = eTagService.getETag(jsonObject);
        jedis.hset(key, "eTag", eTag);
        return eTag;
    }

    public String createPlan(JSONObject plan, String key) {
        jsonToMap(plan);
        return setETag(key, plan);
    }

    public JSONObject getPlan(String key) {
        Map<String, String> value = jedis.hgetAll(key);
        jedis.close();

        return mapToJson(value);
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
                jedis.sadd(redisKey + ":" + key, ((Map<String, Map<String, Object>>) value).keySet().stream().findFirst() + "");
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

    public List<Object> jsonToList(JSONArray jsonArray) {
        List<Object> result = new ArrayList<>();
        for (Object value : jsonArray) {
            if (value instanceof JSONArray) value = jsonToList((JSONArray) value);
            else if (value instanceof JSONObject) value = jsonToMap((JSONObject) value);
            result.add(value);
        }
        return result;
    }

    public JSONObject mapToJson(Map<String, String> map){
        Map<String, Object> result = new HashMap<>();
        for(Map.Entry<String, String> entry: map.entrySet()){
            try {
                int value = Integer.parseInt(entry.getValue());
                result.put(entry.getKey(), value);
            } catch (Exception e) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return new JSONObject(result);
    }
}
