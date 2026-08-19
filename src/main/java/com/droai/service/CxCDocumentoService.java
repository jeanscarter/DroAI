package com.droai.service;

import com.droai.dao.CxCDocumentoDAO;
import com.droai.model.CxCDocumentoRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de negocio para el módulo de Estado de Cuentas por Cobrar (CxC).
 */
public class CxCDocumentoService {

    private final CxCDocumentoDAO dao;

    public CxCDocumentoService() {
        this.dao = new CxCDocumentoDAO();
    }

    public List<CxCDocumentoRow> consultarDocumentos(LocalDate desde, LocalDate hasta, LocalDate fechaCorte) {
        if (desde == null) desde = LocalDate.of(2020, 1, 1);
        if (hasta == null) hasta = LocalDate.now();
        if (fechaCorte == null) fechaCorte = hasta;

        return dao.fetchDocumentosCxC(desde, hasta, fechaCorte);
    }

    public String getDbLabel(LocalDate desde, LocalDate hasta) {
        return CxCDocumentoDAO.getDbLabel(desde, hasta);
    }

    public static class TotalesCxC {
        private int totalRegistros;
        private double totalNeto;
        private double totalNetoBs;
        private double totalIva;
        private double totalIvaBs;
        private double totalSaldo;
        private double totalBs;
        private double totalPorVencer;
        private double totalPorVencerBs;
        private double total1a30;
        private double total31a60;
        private double total61a90;
        private double totalMas91;
        private double totalVencido;
        private double totalVencidoBs;
        private double porcVencido;

        // Getters y Setters
        public int getTotalRegistros() { return totalRegistros; }
        public void setTotalRegistros(int totalRegistros) { this.totalRegistros = totalRegistros; }

        public double getTotalNeto() { return totalNeto; }
        public void setTotalNeto(double totalNeto) { this.totalNeto = totalNeto; }

        public double getTotalNetoBs() { return totalNetoBs; }
        public void setTotalNetoBs(double totalNetoBs) { this.totalNetoBs = totalNetoBs; }

        public double getTotalIva() { return totalIva; }
        public void setTotalIva(double totalIva) { this.totalIva = totalIva; }

        public double getTotalIvaBs() { return totalIvaBs; }
        public void setTotalIvaBs(double totalIvaBs) { this.totalIvaBs = totalIvaBs; }

        public double getTotalSaldo() { return totalSaldo; }
        public void setTotalSaldo(double totalSaldo) { this.totalSaldo = totalSaldo; }

        public double getTotalBs() { return totalBs; }
        public void setTotalBs(double totalBs) { this.totalBs = totalBs; }

        public double getTotalPorVencer() { return totalPorVencer; }
        public void setTotalPorVencer(double totalPorVencer) { this.totalPorVencer = totalPorVencer; }

        public double getTotalPorVencerBs() { return totalPorVencerBs; }
        public void setTotalPorVencerBs(double totalPorVencerBs) { this.totalPorVencerBs = totalPorVencerBs; }

        public double getTotal1a30() { return total1a30; }
        public void setTotal1a30(double total1a30) { this.total1a30 = total1a30; }

        public double getTotal31a60() { return total31a60; }
        public void setTotal31a60(double total31a60) { this.total31a60 = total31a60; }

        public double getTotal61a90() { return total61a90; }
        public void setTotal61a90(double total61a90) { this.total61a90 = total61a90; }

        public double getTotalMas91() { return totalMas91; }
        public void setTotalMas91(double totalMas91) { this.totalMas91 = totalMas91; }

        public double getTotalVencido() { return totalVencido; }
        public void setTotalVencido(double totalVencido) { this.totalVencido = totalVencido; }

        public double getTotalVencidoBs() { return totalVencidoBs; }
        public void setTotalVencidoBs(double totalVencidoBs) { this.totalVencidoBs = totalVencidoBs; }

        public double getPorcVencido() { return porcVencido; }
        public void setPorcVencido(double porcVencido) { this.porcVencido = porcVencido; }
    }

    public TotalesCxC calcularTotales(List<CxCDocumentoRow> rows) {
        TotalesCxC t = new TotalesCxC();
        if (rows == null || rows.isEmpty()) {
            return t;
        }

        t.setTotalRegistros(rows.size());
        double sumNeto = 0;
        double sumNetoBs = 0;
        double sumIva = 0;
        double sumIvaBs = 0;
        double sumSaldo = 0;
        double sumBs = 0;
        double sumPorVencer = 0;
        double sumPorVencerBs = 0;
        double sum1a30 = 0;
        double sum31a60 = 0;
        double sum61a90 = 0;
        double sumMas91 = 0;

        for (CxCDocumentoRow r : rows) {
            double tasa = r.getTasa() > 0 ? r.getTasa() : 1.0;
            sumNeto += r.getNeto();
            sumNetoBs += r.getNeto() * tasa;
            sumIva += r.getIva();
            sumIvaBs += r.getIva() * tasa;
            sumSaldo += r.getSaldo();
            sumBs += r.getTotalBs();
            sumPorVencer += r.getPorVencer();
            sumPorVencerBs += r.getPorVencer() * tasa;
            sum1a30 += r.getVencido1a30();
            sum31a60 += r.getVencido31a60();
            sum61a90 += r.getVencido61a90();
            sumMas91 += r.getVencidoMas91();
        }

        double sumVencido = sum1a30 + sum31a60 + sum61a90 + sumMas91;
        double sumVencidoBs = sumBs - sumPorVencerBs;
        double porcVenc = (sumSaldo > 0) ? (sumVencido / sumSaldo) * 100.0 : 0.0;

        t.setTotalNeto(sumNeto);
        t.setTotalNetoBs(sumNetoBs);
        t.setTotalIva(sumIva);
        t.setTotalIvaBs(sumIvaBs);
        t.setTotalSaldo(sumSaldo);
        t.setTotalBs(sumBs);
        t.setTotalPorVencer(sumPorVencer);
        t.setTotalPorVencerBs(sumPorVencerBs);
        t.setTotal1a30(sum1a30);
        t.setTotal31a60(sum31a60);
        t.setTotal61a90(sum61a90);
        t.setTotalMas91(sumMas91);
        t.setTotalVencido(sumVencido);
        t.setTotalVencidoBs(sumVencidoBs);
        t.setPorcVencido(porcVenc);

        return t;
    }

    public static class ResumenAgrupado {
        private String grupo;
        private double porVencer;
        private double vencido1a30;
        private double vencido31a60;
        private double vencido61a90;
        private double vencidoMas91;
        private double saldoTotal;
        private double vencidoTotal;
        private double porcVencido;

        public String getGrupo() { return grupo; }
        public void setGrupo(String grupo) { this.grupo = grupo; }

        public double getPorVencer() { return porVencer; }
        public void setPorVencer(double porVencer) { this.porVencer = porVencer; }

        public double getVencido1a30() { return vencido1a30; }
        public void setVencido1a30(double vencido1a30) { this.vencido1a30 = vencido1a30; }

        public double getVencido31a60() { return vencido31a60; }
        public void setVencido31a60(double vencido31a60) { this.vencido31a60 = vencido31a60; }

        public double getVencido61a90() { return vencido61a90; }
        public void setVencido61a90(double vencido61a90) { this.vencido61a90 = vencido61a90; }

        public double getVencidoMas91() { return vencidoMas91; }
        public void setVencidoMas91(double vencidoMas91) { this.vencidoMas91 = vencidoMas91; }

        public double getSaldoTotal() { return saldoTotal; }
        public void setSaldoTotal(double saldoTotal) { this.saldoTotal = saldoTotal; }

        public double getVencidoTotal() { return vencidoTotal; }
        public void setVencidoTotal(double vencidoTotal) { this.vencidoTotal = vencidoTotal; }

        public double getPorcVencido() { return porcVencido; }
        public void setPorcVencido(double porcVencido) { this.porcVencido = porcVencido; }
    }

    public List<ResumenAgrupado> agruparPorVendedor(List<CxCDocumentoRow> rows) {
        return agruparPorVendedor(rows, false);
    }

    /**
     * Agrupa por vendedor para generar tablas de resumen en USD (isBs=false) o Bs (isBs=true).
     */
    public List<ResumenAgrupado> agruparPorVendedor(List<CxCDocumentoRow> rows, boolean isBs) {
        Map<String, ResumenAgrupado> map = new LinkedHashMap<>();
        for (CxCDocumentoRow r : rows) {
            String vend = r.getNombreVendedor() != null && !r.getNombreVendedor().isBlank() 
                    ? r.getNombreVendedor() : "SIN ASIGNAR";
            ResumenAgrupado item = map.computeIfAbsent(vend, k -> {
                ResumenAgrupado ra = new ResumenAgrupado();
                ra.setGrupo(k);
                return ra;
            });
            double factor = isBs ? (r.getTasa() > 0 ? r.getTasa() : 1.0) : 1.0;
            item.setPorVencer(item.getPorVencer() + (r.getPorVencer() * factor));
            item.setVencido1a30(item.getVencido1a30() + (r.getVencido1a30() * factor));
            item.setVencido31a60(item.getVencido31a60() + (r.getVencido31a60() * factor));
            item.setVencido61a90(item.getVencido61a90() + (r.getVencido61a90() * factor));
            item.setVencidoMas91(item.getVencidoMas91() + (r.getVencidoMas91() * factor));
            item.setSaldoTotal(item.getSaldoTotal() + (isBs ? r.getTotalBs() : r.getSaldo()));
        }

        for (ResumenAgrupado ra : map.values()) {
            double vTot = ra.getVencido1a30() + ra.getVencido31a60() + ra.getVencido61a90() + ra.getVencidoMas91();
            ra.setVencidoTotal(vTot);
            ra.setPorcVencido(ra.getSaldoTotal() > 0 ? (vTot / ra.getSaldoTotal()) * 100.0 : 0.0);
        }

        return new ArrayList<>(map.values());
    }

    public List<ResumenAgrupado> agruparPorAnalista(List<CxCDocumentoRow> rows) {
        return agruparPorAnalista(rows, false);
    }

    public List<ResumenAgrupado> agruparPorAnalista(List<CxCDocumentoRow> rows, boolean isBs) {
        Map<String, ResumenAgrupado> map = new LinkedHashMap<>();
        for (CxCDocumentoRow r : rows) {
            String analista = r.getAnalista() != null && !r.getAnalista().isBlank() 
                    ? r.getAnalista() : "(Sin Analista)";
            ResumenAgrupado item = map.computeIfAbsent(analista, k -> {
                ResumenAgrupado ra = new ResumenAgrupado();
                ra.setGrupo(k);
                return ra;
            });
            double factor = isBs ? (r.getTasa() > 0 ? r.getTasa() : 1.0) : 1.0;
            item.setPorVencer(item.getPorVencer() + (r.getPorVencer() * factor));
            item.setVencido1a30(item.getVencido1a30() + (r.getVencido1a30() * factor));
            item.setVencido31a60(item.getVencido31a60() + (r.getVencido31a60() * factor));
            item.setVencido61a90(item.getVencido61a90() + (r.getVencido61a90() * factor));
            item.setVencidoMas91(item.getVencidoMas91() + (r.getVencidoMas91() * factor));
            item.setSaldoTotal(item.getSaldoTotal() + (isBs ? r.getTotalBs() : r.getSaldo()));
        }

        for (ResumenAgrupado ra : map.values()) {
            double vTot = ra.getVencido1a30() + ra.getVencido31a60() + ra.getVencido61a90() + ra.getVencidoMas91();
            ra.setVencidoTotal(vTot);
            ra.setPorcVencido(ra.getSaldoTotal() > 0 ? (vTot / ra.getSaldoTotal()) * 100.0 : 0.0);
        }

        return new ArrayList<>(map.values());
    }
}
