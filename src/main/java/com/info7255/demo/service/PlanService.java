package com.info7255.demo.service;


import org.json.JSONObject;

import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Service
public class PlanService {
    private final Jedis jedis;

    public PlanService(Jedis jedis) {
        this.jedis = jedis;
    }

    public boolean isKeyPresent(String key) {
        Map<String, String> value = jedis.hgetAll(key);
        jedis.close();
        return !(value == null || value.isEmpty());
    }

    public String createPlan(JSONObject plan, String objectType) {
//        String key = (String) plan.get("objectId");
//        jedis.hset(key, jsonToMap(plan));
//        jedis.close();
//
//        return key;
        ArrayList<String> keysToDelete = new ArrayList<>();
        for (String key : plan.keySet()) {
            Object current = plan.get(key);
            if (current instanceof JSONObject) {
                String objectKey = createPlan((JSONObject) current, key);
                keysToDelete.add(key);

                String relationKey = objectType + ":" + plan.getString("objectId") + ":relation";
                jedis.sadd(relationKey, objectKey);
                jedis.close();
            }
        }

        // Remove objects from json that are stored separately
        for (String key : keysToDelete) {
            plan.remove(key);
        }
        //save the current object in redis
        String objectKey = objectType + ":" + plan.get("objectId");
        jedis.hset(objectKey, jsonToMap(plan));
        return objectKey;
    }

    public JSONObject getPlan(String key) {
        Map<String, String> value = jedis.hgetAll(key);
        jedis.close();

        return new JSONObject(value);
    }

    public void deletePlan(String key) {
        jedis.del(key);
        jedis.close();
    }

    public Map<String, String> jsonToMap(JSONObject jsonObject) {
        Map<String, String> map = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            map.put(key, jsonObject.get(key).toString());
        }
        return map;
    }
}
