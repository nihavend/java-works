package com.tabii.redisclient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/redis")
public class RedisController {

    private final RedisService redisService;

    public RedisController(RedisService redisService) {
        this.redisService = redisService;
    }

    @PostMapping("/set")
    public String set(@RequestParam String key, @RequestParam String value) {
        redisService.saveValue(key, value);
        return "Saved!";
    }

    @GetMapping("/get")
    public String get(@RequestParam String key) {
        return redisService.getValue(key);
    }

    @DeleteMapping("/del")
    public String delete(@RequestParam String key) {
        redisService.deleteKey(key);
        return "Deleted!";
    }
}
