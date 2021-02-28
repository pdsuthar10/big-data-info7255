package com.info7255.demo.service;


import org.json.JSONObject;

import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;


@Service
public class PlanService {
    private final Jedis jedis;

    public PlanService(Jedis jedis) {
        this.jedis = jedis;
    }


    public boolean isKeyPresent( String key ) {
        String value = jedis.get(key);
        jedis.close();
        return !(value == null || value.isEmpty());
    }

    public String createPlan( JSONObject plan ) {
        String key = (String) plan.get("objectId");
        jedis.set(key, plan.toString());
        jedis.close();

        return key;
    }

    public JSONObject getPlan( String key ) {
        String value = jedis.get(key);
        jedis.close();

        return new JSONObject(value);
    }

    public void deletePlan( String key ) {
        jedis.del(key);
        jedis.close();
    }
}
