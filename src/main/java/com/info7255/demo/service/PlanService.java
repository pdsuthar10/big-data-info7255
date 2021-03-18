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
        ArrayList<String> keysToDelete = new ArrayList<>();
        for (String key : plan.keySet()) {
            Object current = plan.get(key);
            if (current instanceof JSONObject) {
                String objectKey = createPlan((JSONObject) current, key);
                keysToDelete.add(key);

                String relationKey = objectType + ":" + plan.getString("objectId") + ":relation";
                jedis.sadd(relationKey, objectKey);
                System.out.println(jedis.smembers(relationKey));
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

        return mapToJson(value);
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
