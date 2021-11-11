package me.voltbox.hackergy.restapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.voltbox.hackergy.common.domain.EnrichedGrantDto;
import me.voltbox.hackergy.common.domain.GrantFilterDto;
import me.voltbox.hackergy.common.service.DatastoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RestApiController {

    private final DatastoreService datastoreService;

    @GetMapping(value = "/api")
    @ResponseBody
    public ResponseEntity<List<EnrichedGrantDto>> findByFilter(GrantFilterDto filter) {
        return ResponseEntity.ok(StreamSupport.stream(datastoreService.findByFilter(filter).spliterator(), false).toList());
    }
}
