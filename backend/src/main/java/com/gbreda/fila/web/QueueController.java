package com.gbreda.fila.web;

import com.gbreda.fila.service.QueueService;
import com.gbreda.fila.web.dto.ApiModels.QueueResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueService queue;

    public QueueController(QueueService queue) {
        this.queue = queue;
    }

    @GetMapping
    public QueueResponse queue(@RequestParam(defaultValue = "newest") String sort) {
        return queue.queue(sort);
    }
}
