package com.viandas.api.notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class OrderEventBroadcaster {
	private final Map<Long, List<SseEmitter>> emittersByCompany = new ConcurrentHashMap<>();

	public SseEmitter subscribe(Long companyId) {
		SseEmitter emitter = new SseEmitter(0L);
		register(companyId, emitter);
		return emitter;
	}

	public SseEmitter subscribe(List<Long> companyIds) {
		SseEmitter emitter = new SseEmitter(0L);
		companyIds.forEach(companyId -> register(companyId, emitter));
		return emitter;
	}

	private void register(Long companyId, SseEmitter emitter) {
		emittersByCompany.computeIfAbsent(companyId, ignored -> new ArrayList<>()).add(emitter);
		emitter.onCompletion(() -> remove(companyId, emitter));
		emitter.onTimeout(() -> remove(companyId, emitter));
		emitter.onError(error -> remove(companyId, emitter));
	}

	public void publish(Long companyId, String eventName, Object payload) {
		List<SseEmitter> emitters = emittersByCompany.getOrDefault(companyId, List.of());
		List<SseEmitter> dead = new ArrayList<>();
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event().name(eventName).data(payload));
			} catch (IOException | IllegalStateException exception) {
				dead.add(emitter);
			}
		}
		dead.forEach(emitter -> remove(companyId, emitter));
	}

	private void remove(Long companyId, SseEmitter emitter) {
		List<SseEmitter> emitters = emittersByCompany.get(companyId);
		if (emitters != null) {
			emitters.remove(emitter);
		}
	}
}
