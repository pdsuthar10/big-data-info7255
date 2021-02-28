package com.info7255.demo.service;


import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;


@Service
public class PlanService {

    @Autowired
    JedisPool jedisPool;

    public boolean isKeyPresent(String key) {
        Jedis jedis = jedisPool.getResource();
        String value = jedis.get(key);
        jedis.close();
        return !(value == null || value.isEmpty());
    }

    public String createPlan(JSONObject plan, String objectType) {
//        String key = (String) plan.get("objectId");
//        Jedis jedis = this.getJedisPool().getResource();
//        jedis.set(key, plan.toString());
//        jedis.close();
//
//        return key;
        ArrayList<String> keysToDelete = new ArrayList<>();
        for (String key : plan.keySet()) {
            Object current = plan.get(key);
            if (current instanceof JSONObject){
                String objectKey = createPlan((JSONObject) current, key);
                keysToDelete.add(key);

                Jedis jedis = getJedisPool().getResource();

            }
        }

    }

    public JSONObject getPlan(String key) {
        Jedis jedis = this.getJedisPool().getResource();
        String value = jedis.get(key);
        jedis.close();

        return new JSONObject(value);
    }

    public void deletePlan(String key) {
        Jedis jedis = this.getJedisPool().getResource();
        jedis.del(key);
        jedis.close();
    }
}
