package mx.fnconcretos.comercial.service;

import lombok.RequiredArgsConstructor;
import mx.fnconcretos.comercial.dto.response.BitacoraEventoResponse;
import mx.fnconcretos.comercial.entity.BitacoraEvento;
import mx.fnconcretos.comercial.repository.BitacoraEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BitacoraService {

    private final BitacoraEventoRepository bitacoraEventoRepository;

    @Transactional
    public void registrar(String entidadTipo, Long entidadId, String accion, String descripcion, String usuario) {
        bitacoraEventoRepository.save(BitacoraEvento.builder()
                .entidadTipo(entidadTipo)
                .entidadId(entidadId)
                .accion(accion)
                .descripcion(descripcion)
                .usuario(usuario)
                .build());
    }

    @Transactional(readOnly = true)
    public List<BitacoraEventoResponse> listar(String entidadTipo, Long entidadId) {
        return bitacoraEventoRepository.findByEntidadTipoAndEntidadIdOrderByCreadoEnDesc(entidadTipo, entidadId).stream()
                .map(e -> BitacoraEventoResponse.builder()
                        .id(e.getId())
                        .accion(e.getAccion())
                        .descripcion(e.getDescripcion())
                        .usuario(e.getUsuario())
                        .creadoEn(e.getCreadoEn())
                        .build())
                .toList();
    }
}
