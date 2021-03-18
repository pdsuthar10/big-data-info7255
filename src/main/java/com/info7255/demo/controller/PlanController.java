package com.info7255.demo.controller;

import com.info7255.demo.exception.*;
import com.info7255.demo.model.JwtResponse;
import com.info7255.demo.service.ETagService;
import com.info7255.demo.service.PlanService;
import com.info7255.demo.util.JwtUtil;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PlanController {
    private PlanService planService;
    private ETagService eTagService;
    private final JwtUtil jwtUtil;

    public PlanController(PlanService planService, ETagService eTagService, JwtUtil jwtUtil) {
        this.planService = planService;
        this.eTagService = eTagService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/generateToken")
    public JwtResponse generateToken(){
        String token = jwtUtil.generateToken();
        return new JwtResponse(token);
    }

    @PostMapping("/validate")
    public boolean validateToken(@RequestHeader HttpHeaders requestHeader){
        boolean result;
        String authorization = requestHeader.getFirst("Authorization");
        if (authorization == null || authorization.isBlank()) throw new UnauthorizedException("Missing token!");
        try {
            String token = authorization.split(" ")[1];
            result = jwtUtil.validateToken(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid Token");
        }
       return result;
    }

    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPlan(@RequestBody String planObject) {
        JSONObject plan = new JSONObject(planObject);
//        JSONObject schemaJSON = new JSONObject(new JSONTokener(PlanController.class.getResourceAsStream("/plan-schema.json")));
//        Schema schema = SchemaLoader.load(schemaJSON);
//        try {
//            schema.validate(plan);
//        } catch (ValidationException e) {
//            throw new BadRequestException(e.getMessage());
//        }

        String keyToSearch = "plan:" + plan.getString("objectId");
        if (planService.isKeyPresent(keyToSearch)) throw new ConflictException("Plan already exists!");

        String objectId = planService.createPlan(plan, "plan");
        String eTag = eTagService.getETag(plan);
        HttpHeaders headersToSend = new HttpHeaders();
        headersToSend.setETag(eTag);

        return new ResponseEntity<>("{\"objectId\": \"" + objectId + "\"}", headersToSend, HttpStatus.CREATED);
    }

    @GetMapping(value = "/plan/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPlan(@PathVariable String objectId, @RequestHeader HttpHeaders headers) {
        if (!planService.isKeyPresent(objectId)) throw new ResourceNotFoundException("Object not found!");

        JSONObject object = planService.getPlan(objectId);
        String eTag = eTagService.getETag(object);
        HttpHeaders headersToSend = new HttpHeaders();
        headersToSend.setETag(eTag);


        List<String> ifNoneMatch;
        try {
            ifNoneMatch = headers.getIfNoneMatch();
        } catch (Exception e) {
            throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
        }

        return (eTagService.verifyETag(object, ifNoneMatch)) ?
                (new ResponseEntity<>(null, headersToSend, HttpStatus.NOT_MODIFIED))
                :
                (new ResponseEntity<>(object.toString(), headersToSend, HttpStatus.OK));
    }

    @DeleteMapping("/plan/{objectId}")
    public ResponseEntity<?> deletePlan(@PathVariable String objectId) {
        if (!planService.isKeyPresent(objectId)) throw new ResourceNotFoundException("Plan not found!");

        planService.deletePlan(objectId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
