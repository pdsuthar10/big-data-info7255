package com.info7255.demo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.info7255.demo.service.PlanService;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlanController {
    private PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createPlan(@RequestBody String planObject) throws JsonProcessingException {
        JSONObject object = new JSONObject(planObject);
        String objectKey = planService.createPlan(object, "plan");
        return "{\"objectId\": \"" + objectKey + "\"}";
    }

    @GetMapping("/plan/{objectId}")
    public JSONObject getPlan(@PathVariable String objectId){
        return planService.getPlan(objectId);
    }
}
