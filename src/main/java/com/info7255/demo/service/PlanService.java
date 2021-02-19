package com.info7255.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

@Service
public class PlanService {
    private JedisPool jedisPool;

    private JedisPool getJedisPool() {
        if (this.jedisPool == null) {
            this.jedisPool = new JedisPool();
        }
        return this.jedisPool;
    }

    public String createPlan(JSONObject planObject, String objectType) throws JsonProcessingException {
        ArrayList<String> keysToRemove = new ArrayList<String>();
        for(String key : planObject.keySet()){
            Object current = planObject.get(key);
            if(current instanceof JSONObject) {
                String objectKey = createPlan((JSONObject) current, key);
                keysToRemove.add(key);

                Jedis jedis = this.getJedisPool().getResource();
                String relationKey = objectKey + "_" + planObject.get("objectId") + "_" + key.replace("-","");
                String relationKey = objectType + "_" + planObject.get("objectId");
                String relationValue = key + "_" + objectKey;
                jedis.sadd(relationKey, relationValue);
                jedis.close();
                System.out.println("From JSONObject:");
                System.out.println("Relation: "+relationKey+" -> "+relationValue);
                System.out.println("--------------------");

            }else if (current instanceof JSONArray) {
                JSONArray currentArrayValue = (JSONArray)current;
                //temp array to store keys of individual objects
                String[] tempValues = new String[currentArrayValue.length()];

                //iterate through the array
                for (int i = 0; i < currentArrayValue.length(); i++) {
                    if (currentArrayValue.get(i) instanceof JSONObject) {
                        JSONObject arrayObject = (JSONObject)currentArrayValue.get(i);
                        String arrayObjectKey = createPlan(arrayObject, (String)arrayObject.get("objectType"));

                        tempValues[i] = arrayObjectKey.replace("-","");
                    }
                }

                keysToRemove.add(key);

                // save the Array as separate key in redis
                Jedis jedis = this.getJedisPool().getResource();
                String relationKey = objectType + "_"
                        + planObject.get("objectId").toString().replace("-","")
                        + "_" + key.replace("-","");
                System.out.println("Relation Key: "+ relationKey +"Arrays: " + Arrays.toString(tempValues));
                jedis.sadd(relationKey, Arrays.toString(tempValues));
                jedis.close();

//                System.out.println("from array:");
//                System.out.println(relationKey+": "+Arrays.toString(tempValues));
//                System.out.println("------------");

            }
        }

        for (String key : keysToRemove) {
            planObject.remove(key);
        }

        String objectKey = planObject.get("objectId").toString().replace("-","");
        Map<String, String> map = convertJSONToMap(planObject);
        Jedis jedis = this.getJedisPool().getResource();
//        System.out.println("Object key: "+objectKey+" MAP: "+map);
//        System.out.println("--------------------------");
        jedis.hset(objectKey, map);
        jedis.close();
//        System.out.println("Jedis get: " + jedis.hgetAll("12xvxc345ssdsds508"));
//        System.out.println("Jedis smembers: " + jedis.smembers("plan:12xvxc345ssdsds508"));
        return objectKey;
    }

    public JSONObject getPlan(String planKey){
        JedisPool jedisPool = new JedisPool();
        Jedis jedis;
        JSONObject json;
        if (isStringArray(planKey)) {
            ArrayList<JSONObject> arrayValue = getFromArrayString(planKey);
            json = new JSONObject(arrayValue);
        } else {
            jedis = jedisPool.getResource();
            String jsonString = jedis.get(planKey);
            jedis.close();
            if (jsonString == null || jsonString.isEmpty()) {
                return null;
            }
            json = new JSONObject(jsonString);
        }
        jedis = jedisPool.getResource();
        Set<String> jsonRelatedKeys = jedis.keys(planKey + "_*");
        jedis.close();
        return new JSONObject();
    }

    public Map<String, String> convertJSONToMap(JSONObject object){
        Map<String, String> result = new HashMap<>();
        for(String key: object.keySet()){
            result.put(key, object.get(key) + "");
        }
        return result;
    }

    private boolean isStringArray(String str) {
        if (str.indexOf('[') < str.indexOf(']')) {
            if (str.substring((str.indexOf('[') + 1), str.indexOf(']')).split(", ").length > 0)
                return true;
            else
                return false;
        } else {
            return false;
        }
    }

    private ArrayList<JSONObject> getFromArrayString(String keyArray) {
        ArrayList<JSONObject> jsonArray = new ArrayList<>();
        String[] array = keyArray.substring((keyArray.indexOf('[') + 1), keyArray.indexOf(']')).split(", ");

        for (String key : array) {
            JSONObject partObj = this.getPlan(key);
            jsonArray.add(partObj);
        }

        return jsonArray;
    }
}
