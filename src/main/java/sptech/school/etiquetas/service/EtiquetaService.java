package sptech.school.etiquetas.service;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sptech.school.etiquetas.client.ItemClient;
import sptech.school.etiquetas.dto.ItemEtiquetaResponseDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class EtiquetaService {

    private final ItemClient itemClient;

    public EtiquetaService(ItemClient itemClient) {
        this.itemClient = itemClient;
    }

    public byte[] gerarEtiquetaPdf(Integer itemId, String authorizationHeader) {
        ItemEtiquetaResponseDto item = itemClient.buscarItemPorId(itemId, authorizationHeader);

        try (InputStream jrxmlStream = getClass().getResourceAsStream("/etiqueta.jrxml")) {
            if (jrxmlStream == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Template 'etiqueta.jrxml' não encontrado no classpath.");
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("codigoInterno", item.codigoInterno() != null ? item.codigoInterno() : "");
            parameters.put("marca", item.marca() != null ? item.marca() : "");
            parameters.put("descricao", item.descricao() != null ? item.descricao() : "");
            parameters.put("dataCadastro", item.dataCadastro() != null ? item.dataCadastro().toString() : "");

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
            exporter.exportReport();

            return outputStream.toByteArray();
        } catch (JRException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar etiqueta PDF.", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler o template da etiqueta.", ex);
        }
    }
}
