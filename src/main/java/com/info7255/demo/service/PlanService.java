package com.info7255.demo.service;


import org.json.JSONObject;

import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


@Service
public class PlanService {
    private JedisPool jedisPool;

    private JedisPool getJedisPool() {
        if (this.jedisPool == null) {
            this.jedisPool = new JedisPool();
        }
        return this.jedisPool;
    }

    public boolean isKeyPresent(String key){
        Jedis jedis = this.getJedisPool().getResource();
        String value = jedis.get(key);
        jedis.close();
        return  !( value == null || value.isEmpty() );
    }

    public String createPlan( JSONObject plan ) {
        String key = (String) plan.get("objectId");
        Jedis jedis = this.getJedisPool().getResource();
        jedis.set(key, plan.toString());
        jedis.close();

        return key;
    }

    public JSONObject getPlan( String key ) {
        Jedis jedis = this.getJedisPool().getResource();
        String value = jedis.get(key);
        jedis.close();

        return new JSONObject(value);
    }

    public void deletePlan( String key ) {
        Jedis jedis = this.getJedisPool().getResource();
        jedis.del(key);
        jedis.close();
    }
}
