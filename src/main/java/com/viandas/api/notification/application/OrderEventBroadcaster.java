package com.viandas.api.notification.application;

import java.util.UUID;

import com.viandas.api.notification.domain.*;
import com.viandas.api.notification.persistence.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class OrderEventBroadcaster {
	private final Map<UUID, List<SseEmitter>> emittersByCompany = new ConcurrentHashMap<>();

	public SseEmitter subscribe(UUID companyId) {
		SseEmitter emitter = new SseEmitter(0L);
		register(companyId, emitter);
		return emitter;
	}

	public SseEmitter subscribe(List<UUID> companyIds) {
		SseEmitter emitter = new SseEmitter(0L);
		companyIds.forEach(companyId -> register(companyId, emitter));
		return emitter;
	}

	private void register(UUID companyId, SseEmitter emitter) {
		emittersByCompany.computeIfAbsent(companyId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
		emitter.onCompletion(() -> remove(companyId, emitter));
		emitter.onTimeout(() -> remove(companyId, emitter));
		emitter.onError(error -> remove(companyId, emitter));
	}

	public void publish(UUID companyId, String eventName, Object payload) {
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

	private void remove(UUID companyId, SseEmitter emitter) {
		List<SseEmitter> emitters = emittersByCompany.get(companyId);
		if (emitters != null) {
			emitters.remove(emitter);
		}
	}
}
