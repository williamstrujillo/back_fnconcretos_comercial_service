package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.request.ContactoRequest;
import mx.fnconcretos.comercial.dto.response.ContactoResponse;
import mx.fnconcretos.comercial.entity.Cliente;
import mx.fnconcretos.comercial.entity.ContactoCliente;
import mx.fnconcretos.comercial.exception.ResourceNotFoundException;
import mx.fnconcretos.comercial.repository.ContactoClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactoClienteService {

    private final ContactoClienteRepository contactoRepository;
    private final ClienteService clienteService;

    @Transactional(readOnly = true)
    public List<ContactoResponse> listarPorCliente(Long clienteId) {
        clienteService.buscarOFallar(clienteId);
        return contactoRepository.findByClienteId(clienteId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ContactoResponse crear(Long clienteId, ContactoRequest request) {
        Cliente cliente = clienteService.buscarOFallar(clienteId);

        ContactoCliente contacto = ContactoCliente.builder()
                .cliente(cliente)
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .correo(request.getCorreo())
                .cargo(request.getCargo())
                .esGenericoCorporativo(request.getEsGenericoCorporativo() != null ? request.getEsGenericoCorporativo() : false)
                .observaciones(request.getObservaciones())
                .build();

        return toResponse(contactoRepository.save(contacto));
    }

    @Transactional
    public ContactoResponse actualizar(Long clienteId, Long contactoId, ContactoRequest request) {
        ContactoCliente contacto = buscarOFallar(clienteId, contactoId);

        contacto.setNombre(request.getNombre());
        contacto.setTelefono(request.getTelefono());
        contacto.setCorreo(request.getCorreo());
        contacto.setCargo(request.getCargo());
        if (request.getEsGenericoCorporativo() != null) contacto.setEsGenericoCorporativo(request.getEsGenericoCorporativo());
        contacto.setObservaciones(request.getObservaciones());

        return toResponse(contactoRepository.save(contacto));
    }

    @Transactional
    public void eliminar(Long clienteId, Long contactoId) {
        ContactoCliente contacto = buscarOFallar(clienteId, contactoId);
        contactoRepository.delete(contacto);
    }

    private ContactoCliente buscarOFallar(Long clienteId, Long contactoId) {
        ContactoCliente contacto = contactoRepository.findById(contactoId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado: " + contactoId));
        if (!contacto.getCliente().getId().equals(clienteId)) {
            throw new ResourceNotFoundException("El contacto " + contactoId + " no pertenece al cliente " + clienteId);
        }
        return contacto;
    }

    private ContactoResponse toResponse(ContactoCliente contacto) {
        return ContactoResponse.builder()
                .id(contacto.getId())
                .clienteId(contacto.getCliente().getId())
                .nombre(contacto.getNombre())
                .telefono(contacto.getTelefono())
                .correo(contacto.getCorreo())
                .cargo(contacto.getCargo())
                .esGenericoCorporativo(contacto.getEsGenericoCorporativo())
                .observaciones(contacto.getObservaciones())
                .createdAt(contacto.getCreatedAt())
                .build();
    }
}
