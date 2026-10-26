package sptech.school.etiquetas.etiqueta.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiqueta;
import sptech.school.etiquetas.etiqueta.domain.SolicitacaoEtiquetaGateway;
import sptech.school.etiquetas.etiqueta.domain.StatusSolicitacaoEtiqueta;

import java.util.Optional;

@Component
public class SolicitacaoEtiquetaJpaAdapter implements SolicitacaoEtiquetaGateway {

    private final SolicitacaoEtiquetaJpaRepository repository;

    public SolicitacaoEtiquetaJpaAdapter(SolicitacaoEtiquetaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SolicitacaoEtiqueta> buscarPorId(String id) {
        return repository.findById(id).map(SolicitacaoEtiquetaEntity::toDomain);
    }

    @Override
    @Transactional
    public boolean iniciarProcessamento(String id, Integer itemId) {
        Optional<SolicitacaoEtiquetaEntity> existente = repository.findById(id);
        if (existente.isEmpty()) {
            repository.save(SolicitacaoEtiquetaEntity.iniciar(id, itemId));
            return true;
        }

        SolicitacaoEtiquetaEntity solicitacao = existente.get();
        if (solicitacao.getStatus() == StatusSolicitacaoEtiqueta.CONCLUIDA) {
            return false;
        }

        solicitacao.marcarProcessando();
        repository.save(solicitacao);
        return true;
    }

    @Override
    @Transactional
    public void marcarConcluida(String id) {
        SolicitacaoEtiquetaEntity solicitacao = buscarEntity(id);
        solicitacao.marcarConcluida();
        repository.save(solicitacao);
    }

    @Override
    @Transactional
    public void marcarErro(String id, String mensagemErro) {
        SolicitacaoEtiquetaEntity solicitacao = buscarEntity(id);
        solicitacao.marcarErro(mensagemErro);
        repository.save(solicitacao);
    }

    private SolicitacaoEtiquetaEntity buscarEntity(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Solicitacao de etiqueta nao encontrada: " + id));
    }
}
