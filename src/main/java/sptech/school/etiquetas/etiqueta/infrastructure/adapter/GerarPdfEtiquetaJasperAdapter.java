package sptech.school.etiquetas.etiqueta.infrastructure.adapter;

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
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import sptech.school.etiquetas.etiqueta.domain.Etiqueta;
import sptech.school.etiquetas.etiqueta.domain.GerarPdfEtiquetaGateway;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class GerarPdfEtiquetaJasperAdapter implements GerarPdfEtiquetaGateway {

    @Override
    public byte[] gerar(Etiqueta etiqueta) {
        try (InputStream jrxmlStream = getClass().getResourceAsStream("/etiqueta.jrxml")) {
            if (jrxmlStream == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Template 'etiqueta.jrxml' não encontrado no classpath.");
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("codigoInterno", etiqueta.codigoInterno());
            parameters.put("marca", etiqueta.marca());
            parameters.put("descricao", etiqueta.descricao());
            parameters.put("dataCadastro", etiqueta.dataCadastro());

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
