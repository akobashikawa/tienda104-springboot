package com.example.tienda104.ventas.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.example.tienda104.application.GenericEvent;
import com.example.tienda104.personas.application.PersonaService;
import com.example.tienda104.personas.domain.Persona;
import com.example.tienda104.productos.application.ProductoService;
import com.example.tienda104.productos.domain.Producto;
import com.example.tienda104.ventas.domain.Venta;
import com.example.tienda104.ventas.domain.VentaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.nats.client.Connection;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class VentaService {

	private final VentaRepository ventaRepository;
	private final ProductoService productoService;
	private final PersonaService personaService;
//	private ApplicationEventPublisher eventPublisher;

	@Autowired
	private Connection natsConnection;

//	public VentaService(VentaRepository ventaRepository, ProductoService productoService, PersonaService personaService,
//			ApplicationEventPublisher eventPublisher) {
	public VentaService(VentaRepository ventaRepository, ProductoService productoService, PersonaService personaService) {
		this.ventaRepository = ventaRepository;
		this.productoService = productoService;
		this.personaService = personaService;
//		this.eventPublisher = eventPublisher;
	}

	public List<Venta> getItems() {
		return ventaRepository.findAll();
	}

	public Optional<Venta> getItemById(Long id) {
		return ventaRepository.findById(id);
	}

	public Venta createItem(VentaDTO ventaDTO) {
		Venta venta = new Venta();
		Producto producto = productoService.getItemById(ventaDTO.getProducto_id())
				.orElseThrow(() -> new RuntimeException("Producto no encontrado"));
		Persona persona = personaService.getItemById(ventaDTO.getPersona_id())
				.orElseThrow(() -> new RuntimeException("Persona no encontrada"));

		venta.setProducto(producto);
		venta.setPersona(persona);
		venta.setPrecio(ventaDTO.getPrecio());
		venta.setCantidad(ventaDTO.getCantidad());
		venta.setFechaHora(LocalDateTime.now());

		Map<String, Object> payload = new HashMap<>();
		payload.put("venta", venta);
		payload.put("producto", producto);
//    	eventPublisher.publishEvent(new GenericEvent(this, "ventaCreate", payload));
		publishEvent("ventaCreate", payload);
		return ventaRepository.save(venta);
	}

	public Venta updateItem(Long id, VentaDTO ventaDTO) {
		Venta venta = this.getItemById(id).orElseThrow(() -> new RuntimeException("Venta no encontrada"));
		Producto producto = productoService.getItemById(ventaDTO.getProducto_id())
				.orElseThrow(() -> new RuntimeException("Producto no encontrado"));
		Persona persona = personaService.getItemById(ventaDTO.getPersona_id())
				.orElseThrow(() -> new RuntimeException("Persona no encontrada"));

		venta.setProducto(producto);
		venta.setPersona(persona);

		if (ventaDTO.getPrecio() != null) {
			venta.setPrecio(ventaDTO.getPrecio());
		}

		int cantidadAnterior = venta.getCantidad();
		if (ventaDTO.getCantidad() != null) {
			venta.setCantidad(ventaDTO.getCantidad());
		}
		venta.setFechaHora(LocalDateTime.now());

		Map<String, Object> payload = new HashMap<>();
		payload.put("venta", venta);
		payload.put("producto", producto);
		payload.put("cantidadAnterior", cantidadAnterior);
//		eventPublisher.publishEvent(new GenericEvent(this, "ventaUpdate", payload));
		publishEvent("ventaUpdate", payload);

		return ventaRepository.save(venta);
	}

	public void deleteItemById(Long id) {
		ventaRepository.deleteById(id);
	}

	private void publishEvent(String subject, Object payload) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.registerModule(new JavaTimeModule());
			objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

			String message = objectMapper.writeValueAsString(payload);
			natsConnection.publish(subject, message.getBytes());
			System.out.println("Venta event published: " + message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
