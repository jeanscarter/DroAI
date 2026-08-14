package com.droai.model;

/**
 * Representa una fila del Maestro de Clientes extraído de Profit Plus.
 * Contiene los 23-24 campos comerciales, fiscales y logísticos de cada cliente.
 */
public class ClienteMaestroRow {

    private String codigo;
    private String rif;
    private String nombre;
    private String nit;
    private String fechaRegistro;
    private String contribuyente;   // "SI" / "NO"
    private String tipoCliente;     // "FARMACIA", "CLINICA", etc.
    private String pais;
    private String zona;
    private String ciudad;
    private String segmento;        // Grupo comercial o "SIN GRUPO"
    private String inactivo;        // "SI" (Inactivo) / "NO" (Activo)
    private String vendedor;
    private String codPostal;
    private String condPago;        // Plazo / condición de crédito
    private String email;
    private String credito;         // "SI" (Tiene crédito) / "NO" (Sin crédito)
    private String telefono;
    private double limiteCredito;
    private String ruta;
    private String tipoPersona;     // "Natural Residente", "Jurídica Domiciliada", etc.
    private String contacto;
    private String direccion;

    public ClienteMaestroRow() {
    }

    public String getCodigo() {
        return codigo != null ? codigo.trim() : "";
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getRif() {
        return rif != null ? rif.trim() : "";
    }

    public void setRif(String rif) {
        this.rif = rif;
    }

    public String getNombre() {
        return nombre != null ? nombre.trim() : "";
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNit() {
        return nit != null ? nit.trim() : "";
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getFechaRegistro() {
        return fechaRegistro != null ? fechaRegistro.trim() : "";
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getContribuyente() {
        return contribuyente != null ? contribuyente.trim() : "NO";
    }

    public void setContribuyente(String contribuyente) {
        this.contribuyente = contribuyente;
    }

    public String getTipoCliente() {
        return tipoCliente != null ? tipoCliente.trim() : "SIN TIPO";
    }

    public void setTipoCliente(String tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public String getPais() {
        return pais != null ? pais.trim() : "Venezuela";
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getZona() {
        return zona != null ? zona.trim() : "SIN ZONA";
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getCiudad() {
        return ciudad != null ? ciudad.trim() : "";
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getSegmento() {
        return segmento != null ? segmento.trim() : "SIN GRUPO";
    }

    public void setSegmento(String segmento) {
        this.segmento = segmento;
    }

    public String getInactivo() {
        return inactivo != null ? inactivo.trim() : "NO";
    }

    public void setInactivo(String inactivo) {
        this.inactivo = inactivo;
    }

    public boolean isActivo() {
        return !"SI".equalsIgnoreCase(getInactivo());
    }

    public String getVendedor() {
        return vendedor != null ? vendedor.trim() : "OFICINA";
    }

    public void setVendedor(String vendedor) {
        this.vendedor = vendedor;
    }

    public String getCodPostal() {
        return codPostal != null ? codPostal.trim() : "";
    }

    public void setCodPostal(String codPostal) {
        this.codPostal = codPostal;
    }

    public String getCondPago() {
        return condPago != null ? condPago.trim() : "CONTADO";
    }

    public void setCondPago(String condPago) {
        this.condPago = condPago;
    }

    public String getEmail() {
        return email != null ? email.trim() : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCredito() {
        return credito != null ? credito.trim() : "NO";
    }

    public void setCredito(String credito) {
        this.credito = credito;
    }

    public String getTelefono() {
        return telefono != null ? telefono.trim() : "";
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public double getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(double limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public String getRuta() {
        return ruta != null ? ruta.trim() : "";
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public String getTipoPersona() {
        return tipoPersona != null ? tipoPersona.trim() : "";
    }

    public void setTipoPersona(String tipoPersona) {
        this.tipoPersona = tipoPersona;
    }

    public String getContacto() {
        return contacto != null ? contacto.trim() : "";
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getDireccion() {
        return direccion != null ? direccion.trim() : "";
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}
