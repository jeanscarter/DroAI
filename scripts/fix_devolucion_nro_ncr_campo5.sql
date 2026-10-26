-- =========================================================================
-- Script: fix_devolucion_nro_ncr_campo5.sql
-- Objetivo:
--   1. Corregir trigger tr_saDevolucionCliente_NoHeredarRutaCampo5 para que
--      guarde en campo5 (Nro. Fiscal en Datos Adicionales) el N° N/CR (nro_doc)
--      y NO el Nro de Control (n_control), evitando además que herede la ruta.
--   2. Corregir los SPs de impresión dRepFormatoDevolucionCliente y
--      dRepFormatoDevolucionClientenew para priorizar nro_doc como N/CR.
--   3. Actualizar los documentos existentes (ej. 0000001140) para reflejar su
--      N° N/CR en campo5.
-- =========================================================================

USE [DROA_A];
GO

-- 1. Actualizar Trigger en saDevolucionCliente
CREATE OR ALTER TRIGGER [dbo].[tr_saDevolucionCliente_NoHeredarRutaCampo5]
ON [dbo].[saDevolucionCliente]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF @@ROWCOUNT = 0
        RETURN;

    -- Prevenir recursión infinita
    IF TRIGGER_NESTLEVEL() > 1
        RETURN;

    -- Caso 1: Si el documento tiene N° N/CR asignado (nro_doc no vacío) y campo5 no coincide con nro_doc
    IF EXISTS (
        SELECT 1 
        FROM inserted i
        WHERE i.nro_doc IS NOT NULL 
          AND LTRIM(RTRIM(i.nro_doc)) <> ''
          AND (i.campo5 IS NULL OR LTRIM(RTRIM(i.campo5)) <> LTRIM(RTRIM(i.nro_doc)))
    )
    BEGIN
        UPDATE d
        SET d.campo5 = LTRIM(RTRIM(i.nro_doc))
        FROM saDevolucionCliente d
        INNER JOIN inserted i ON d.doc_num = i.doc_num
        WHERE i.nro_doc IS NOT NULL 
          AND LTRIM(RTRIM(i.nro_doc)) <> ''
          AND (d.campo5 IS NULL OR LTRIM(RTRIM(d.campo5)) <> LTRIM(RTRIM(i.nro_doc)));
    END;

    -- Caso 2: Si aún no tiene nro_doc asignado pero campo5 contiene una ruta o zona heredada de la factura
    IF EXISTS (
        SELECT 1 
        FROM inserted i
        WHERE (i.nro_doc IS NULL OR LTRIM(RTRIM(i.nro_doc)) = '')
          AND i.campo5 IS NOT NULL 
          AND LTRIM(RTRIM(i.campo5)) <> ''
          AND (
              EXISTS (SELECT 1 FROM saZona z WHERE RTRIM(LTRIM(z.zon_des)) = RTRIM(LTRIM(i.campo5)))
              OR EXISTS (SELECT 1 FROM saFacturaVenta f WHERE RTRIM(LTRIM(f.campo5)) = RTRIM(LTRIM(i.campo5)))
              OR i.campo5 LIKE '%(FALCON)%'
              OR i.campo5 LIKE '%(ZULIA)%'
              OR i.campo5 LIKE '%(TRUJILLO)%'
              OR i.campo5 LIKE '%(MARA)%'
              OR RTRIM(LTRIM(i.campo5)) IN ('EMPLEADO', 'EMPLEADOS', 'NORTE', 'SUR', 'OESTE', 'CENTRO', 'GERENTE COMERCIAL')
          )
    )
    BEGIN
        UPDATE d
        SET d.campo5 = ''
        FROM saDevolucionCliente d
        INNER JOIN inserted i ON d.doc_num = i.doc_num
        WHERE (i.nro_doc IS NULL OR LTRIM(RTRIM(i.nro_doc)) = '')
          AND i.campo5 IS NOT NULL 
          AND LTRIM(RTRIM(i.campo5)) <> ''
          AND (
              EXISTS (SELECT 1 FROM saZona z WHERE RTRIM(LTRIM(z.zon_des)) = RTRIM(LTRIM(i.campo5)))
              OR EXISTS (SELECT 1 FROM saFacturaVenta f WHERE RTRIM(LTRIM(f.campo5)) = RTRIM(LTRIM(i.campo5)))
              OR i.campo5 LIKE '%(FALCON)%'
              OR i.campo5 LIKE '%(ZULIA)%'
              OR i.campo5 LIKE '%(TRUJILLO)%'
              OR i.campo5 LIKE '%(MARA)%'
              OR RTRIM(LTRIM(i.campo5)) IN ('EMPLEADO', 'EMPLEADOS', 'NORTE', 'SUR', 'OESTE', 'CENTRO', 'GERENTE COMERCIAL')
          );
    END;
END;
GO

-- 2. Actualizar Procedimiento dRepFormatoDevolucionCliente
CREATE OR ALTER PROCEDURE [dbo].[dRepFormatoDevolucionCliente] 
    @sCo_Numero_d CHAR(20) = NULL ,
    @sCo_Numero_h CHAR(20) = NULL ,
    @cCo_Sucursal CHAR(6) = NULL ,
    @sCampOrderBy VARCHAR(16) = NULL ,
    @sDir VARCHAR(6) = NULL ,
    @bHeaderRep BIT = 0
AS 
BEGIN
    SET NOCOUNT ON ;

    DECLARE @cCo_Numero_d CHAR(20), @cCo_Numero_h CHAR(20) ;
    DECLARE @Tipo_doc CHAR(11) ;
    SET @Tipo_doc = 'devoventa' ;

    SELECT
        @Tipo_doc AS TIPO_DOC, 'tipo' = 1, ART.modelo, CL.cli_des, CL.rif, CL.nit, CL.telefonos, CL.fax, CL.direc1,
        (CASE WHEN (FV.dir_ent IS NOT NULL AND len(ltrim(FV.dir_ent)) > 0) THEN FV.dir_ent ELSE CL.dir_ent2 END) AS dir_entrega,
        VE.ven_des, TR.des_tran, CP.cond_des, MO.mone_des,
        /*Campos saDevolucionCliente*/ FV.doc_num, FV.descrip, FV.co_cli, FV.co_tran, FV.co_mone, FV.co_ven, FV.co_cond,
        FV.fec_emis, FV.fec_venc, FV.fec_reg, FV.anulado, FV.status, FV.n_control, FV.ven_ter, FV.tasa,
        ISNULL( FV.porc_desc_glob,0) as porc_desc_glob, FV.monto_desc_glob, FV.porc_reca, FV.monto_reca, FV.total_bruto, FV.monto_imp,
        FV.monto_imp2, FV.monto_imp3, FV.otros1, FV.otros2, FV.otros3, FV.total_neto, FV.saldo, FV.dir_ent,
        FV.comentario, FV.dis_cen, FV.feccom, FV.numcom, FV.contrib, FV.impresa, FV.seriales_e, FV.salestax,
        FV.impfis, FV.impfisfac, FV.campo1, FV.campo2, FV.campo3, FV.campo4, 
        CASE 
            WHEN FV.nro_doc IS NOT NULL AND LTRIM(RTRIM(FV.nro_doc)) <> '' THEN LTRIM(RTRIM(FV.nro_doc))
            WHEN FV.campo5 IS NOT NULL AND LTRIM(RTRIM(FV.campo5)) <> '' 
                 AND NOT EXISTS (SELECT 1 FROM saZona z WHERE RTRIM(LTRIM(z.zon_des)) = RTRIM(LTRIM(FV.campo5)))
                 AND NOT EXISTS (SELECT 1 FROM saFacturaVenta f WHERE RTRIM(LTRIM(f.campo5)) = RTRIM(LTRIM(FV.campo5)))
                 AND FV.campo5 NOT LIKE '%(FALCON)%'
                 AND FV.campo5 NOT LIKE '%(ZULIA)%'
                 AND FV.campo5 NOT LIKE '%(TRUJILLO)%'
                 AND FV.campo5 NOT LIKE '%(MARA)%'
                 AND RTRIM(LTRIM(FV.campo5)) NOT IN ('EMPLEADO', 'EMPLEADOS', 'NORTE', 'SUR', 'OESTE', 'CENTRO', 'GERENTE COMERCIAL')
                 THEN LTRIM(RTRIM(FV.campo5))
            WHEN FV.n_control IS NOT NULL AND LTRIM(RTRIM(FV.n_control)) <> '' THEN LTRIM(RTRIM(FV.n_control))
            ELSE LTRIM(RTRIM(FV.doc_num))
        END AS campo5, 
        FV.campo6, FV.campo7,
        FV.campo8, FV.co_us_in, FV.co_sucu_in, FV.fe_us_in, FV.co_us_mo, FV.co_sucu_mo, FV.fe_us_mo, FV.revisado,
        FV.trasnfe, FV.validador, FV.rowguid,
        /*Campos saDevolucionClienteReng*/ FVR.reng_num, FVR.doc_num, FVR.co_art, ISNULL(FVR.des_art, ART.art_des) as des_art, FVR.co_alma, FVR.total_art,
        FVR.stotal_art, FVR.co_uni, FVR.sco_uni, FVR.co_precio, FVR.prec_vta, FVR.prec_vta_om, ISNULL(FVR.porc_desc,0)as porc_desc,
        FVR.monto_desc, FVR.tipo_imp, FVR.tipo_imp2, FVR.tipo_imp3, FVR.porc_imp, FVR.porc_imp2, FVR.porc_imp3,
        FVR.monto_imp, FVR.monto_imp2, FVR.monto_imp3, FVR.reng_neto, FVR.pendiente, FVR.pendiente2, FVR.tipo_doc,
        FVR.num_doc, FAC.fec_emis as fecafe,FAC.total_neto as monafe, FVR.rowguid_doc, FVR.total_dev, FVR.monto_dev, FVR.otros, FVR.comentario, FVR.lote_asignado,
        FVR.dis_cen, FVR.co_us_in, FVR.co_sucu_in, FVR.fe_us_in, FVR.co_us_mo, FVR.co_sucu_mo, FVR.fe_us_mo,
        FVR.revisado, FVR.trasnfe, FVR.rowguid,
        (SELECT i_dec_precio FROM par_emp) as cant_decimales, 
        dbo.ObtenerMonedaBase() as ObtenerMonedaBase,
        FVR.monto_desc_glob as monto_desc_glob_reng
    FROM
        saDevolucionCliente AS FV
        INNER JOIN saDevolucionClienteReng AS FVR ON FVR.doc_num = FV.doc_num
        INNER JOIN saCliente AS CL ON CL.co_cli = FV.co_cli
        INNER JOIN saVendedor AS VE ON VE.co_ven = FV.co_ven
        INNER JOIN saTransporte AS TR ON TR.co_tran = FV.co_tran
        LEFT JOIN saCondicionPago AS CP ON CP.co_cond = FV.co_cond
        INNER JOIN saMoneda AS MO ON MO.co_mone = FV.co_mone
        INNER JOIN saArticulo AS ART ON ART.co_art = FVR.co_art
        INNER JOIN saFacturaVenta as FAC on FAC.doc_num = FVR.num_doc
    WHERE
        (( @sCo_Numero_d IS NULL OR FV.doc_num >= @sCo_Numero_d)
          AND ( @sCo_Numero_h IS NULL OR FV.doc_num <= @sCo_Numero_h))
        AND ( FV.anulado = 0 )
        AND ( @cCo_Sucursal IS NULL OR @cCo_Sucursal = FV.co_sucu_in )
    ORDER BY
        FV.doc_num ASC ;
END ;
GO

-- 3. Actualizar Procedimiento dRepFormatoDevolucionClientenew
CREATE OR ALTER PROCEDURE [dbo].[dRepFormatoDevolucionClientenew] 
    @sCo_Numero_d CHAR(20) = NULL ,
    @sCo_Numero_h CHAR(20) = NULL ,
    @cCo_Sucursal CHAR(6) = NULL ,
    @sCampOrderBy VARCHAR(16) = NULL ,
    @sDir VARCHAR(6) = NULL ,
    @bHeaderRep BIT = 0
AS 
BEGIN
    SET NOCOUNT ON;

    DECLARE @cCo_Numero_d CHAR(20), @cCo_Numero_h CHAR(20);
    DECLARE @Tipo_doc CHAR(11);
    SET @Tipo_doc = 'devoventa';

    SELECT
        @Tipo_doc AS TIPO_DOC, 
        'tipo' = 1, 
        ART.modelo, 
        CL.cli_des, CL.rif, CL.nit, CL.telefonos, CL.fax, CL.direc1,
        (CASE WHEN (FV.dir_ent IS NOT NULL AND len(ltrim(FV.dir_ent)) > 0) THEN FV.dir_ent ELSE CL.dir_ent2 END) AS dir_entrega,
        VE.ven_des, TR.des_tran, CP.cond_des, MO.mone_des,
        /*Campos saDevolucionCliente*/ 
        FV.doc_num, FV.descrip, FV.co_cli, FV.co_tran, FV.co_mone, FV.co_ven, FV.co_cond,
        FV.fec_emis, FV.fec_venc, FV.fec_reg, FV.anulado, FV.status, FV.n_control, FV.ven_ter, FV.tasa,
        ISNULL(FV.porc_desc_glob, 0) AS porc_desc_glob,
        FV.monto_desc_glob, FV.porc_reca, FV.monto_reca, FV.total_bruto, FV.monto_imp,
        FV.monto_imp2, FV.monto_imp3, FV.otros1, FV.otros2, FV.otros3, FV.total_neto, FV.saldo, FV.dir_ent,
        FV.comentario, FV.dis_cen, FV.feccom, FV.numcom, FV.contrib, FV.impresa, FV.seriales_e, FV.salestax,
        FV.impfis, FV.impfisfac, FV.campo1, FV.campo2, FV.campo3, FV.campo4, 
        CASE 
            WHEN FV.nro_doc IS NOT NULL AND LTRIM(RTRIM(FV.nro_doc)) <> '' THEN LTRIM(RTRIM(FV.nro_doc))
            WHEN FV.campo5 IS NOT NULL AND LTRIM(RTRIM(FV.campo5)) <> '' 
                 AND NOT EXISTS (SELECT 1 FROM saZona z WHERE RTRIM(LTRIM(z.zon_des)) = RTRIM(LTRIM(FV.campo5)))
                 AND NOT EXISTS (SELECT 1 FROM saFacturaVenta f WHERE RTRIM(LTRIM(f.campo5)) = RTRIM(LTRIM(FV.campo5)))
                 AND FV.campo5 NOT LIKE '%(FALCON)%'
                 AND FV.campo5 NOT LIKE '%(ZULIA)%'
                 AND FV.campo5 NOT LIKE '%(TRUJILLO)%'
                 AND FV.campo5 NOT LIKE '%(MARA)%'
                 AND RTRIM(LTRIM(FV.campo5)) NOT IN ('EMPLEADO', 'EMPLEADOS', 'NORTE', 'SUR', 'OESTE', 'CENTRO', 'GERENTE COMERCIAL')
                 THEN LTRIM(RTRIM(FV.campo5))
            WHEN FV.n_control IS NOT NULL AND LTRIM(RTRIM(FV.n_control)) <> '' THEN LTRIM(RTRIM(FV.n_control))
            ELSE LTRIM(RTRIM(FV.doc_num))
        END AS campo5, 
        FV.campo6, FV.campo7,
        FV.campo8, FV.co_us_in, FV.co_sucu_in, FV.fe_us_in, FV.co_us_mo, FV.co_sucu_mo, FV.fe_us_mo, FV.revisado,
        FV.trasnfe, FV.validador, FV.rowguid,
        /*Campos saDevolucionClienteReng*/ 
        FVR.reng_num, FVR.doc_num, FVR.co_art, ISNULL(FVR.des_art, ART.art_des) AS des_art, FVR.co_alma, 
        FVR.total_art, FVR.stotal_art, FVR.co_uni, FVR.sco_uni, FVR.co_precio, 
        FVR.prec_vta, FVR.prec_vta_om, ISNULL(FVR.porc_desc,0) AS porc_desc,
        FVR.monto_desc, FVR.tipo_imp, FVR.tipo_imp2, FVR.tipo_imp3, 
        FVR.porc_imp, FVR.porc_imp2, FVR.porc_imp3,
        FVR.monto_imp, FVR.monto_imp2, FVR.monto_imp3, FVR.reng_neto, 
        FVR.pendiente, FVR.pendiente2, FVR.tipo_doc, FVR.num_doc, 
        FAC.fec_emis AS fecafe, FAC.total_neto AS monafe, 
        FVR.rowguid_doc, FVR.total_dev, FVR.monto_dev, FVR.otros, FVR.comentario, 
        FVR.lote_asignado, FVR.dis_cen, FVR.co_us_in, FVR.co_sucu_in, FVR.fe_us_in, 
        FVR.co_us_mo, FVR.co_sucu_mo, FVR.fe_us_mo,
        FVR.revisado, FVR.trasnfe, FVR.rowguid,
        (SELECT i_dec_precio FROM par_emp) AS cant_decimales, 
        dbo.ObtenerMonedaBase() AS ObtenerMonedaBase,
        FVR.monto_desc_glob AS monto_desc_glob_reng,
        v1 AS DG1,         
        v5_limpio AS DG2,  
        v3 AS DG3,         
        v4 AS DG4          
    FROM
        saDevolucionCliente AS FV
        INNER JOIN saDevolucionClienteReng AS FVR ON FVR.doc_num = FV.doc_num
        INNER JOIN saCliente AS CL ON CL.co_cli = FV.co_cli
        INNER JOIN saVendedor AS VE ON VE.co_ven = FV.co_ven
        INNER JOIN saTransporte AS TR ON TR.co_tran = FV.co_tran
        LEFT JOIN saCondicionPago AS CP ON CP.co_cond = FV.co_cond
        INNER JOIN saMoneda AS MO ON MO.co_mone = FV.co_mone
        INNER JOIN saArticulo AS ART ON ART.co_art = FVR.co_art
        INNER JOIN saFacturaVenta AS FAC ON FAC.doc_num = FVR.num_doc
        CROSS APPLY (
            SELECT CASE 
                WHEN FVR.porc_desc IS NULL OR LTRIM(RTRIM(FVR.porc_desc)) = '' THEN '0+0+0+0+0+'
                WHEN (LEN(FVR.porc_desc) - LEN(REPLACE(FVR.porc_desc, '+', ''))) < 4 
                    THEN RTRIM(LTRIM(FVR.porc_desc)) + REPLICATE('+0', 4 - (LEN(FVR.porc_desc) - LEN(REPLACE(FVR.porc_desc, '+', '')))) + '+'
                ELSE RTRIM(LTRIM(FVR.porc_desc)) + '+'
            END
        ) AS pad(pd)
        CROSS APPLY (SELECT CHARINDEX('+', pd) AS p1) ca1
        CROSS APPLY (SELECT LEFT(pd, p1-1) AS v1, STUFF(pd, 1, p1, '') AS r1) s1
        CROSS APPLY (SELECT CHARINDEX('+', r1) AS p2) ca2
        CROSS APPLY (SELECT LEFT(r1, p2-1) AS v2, STUFF(r1, 1, p2, '') AS r2) s2
        CROSS APPLY (SELECT CHARINDEX('+', r2) AS p3) ca3
        CROSS APPLY (SELECT LEFT(r2, p3-1) AS v3, STUFF(r2, 1, p3, '') AS r3) s3
        CROSS APPLY (SELECT CHARINDEX('+', r3) AS p4) ca4
        CROSS APPLY (SELECT LEFT(r3, p4-1) AS v4, STUFF(r3, 1, p4, '') AS r4) s4
        CROSS APPLY (SELECT CASE WHEN RIGHT(r4,1) = '+' THEN LEFT(r4, LEN(r4)-1) ELSE r4 END AS v5_limpio) s5
    WHERE
        (( @sCo_Numero_d IS NULL OR FV.doc_num >= @sCo_Numero_d)
         AND ( @sCo_Numero_h IS NULL OR FV.doc_num <= @sCo_Numero_h))
        AND ( FV.anulado = 0 )
        AND ( @cCo_Sucursal IS NULL OR @cCo_Sucursal = FV.co_sucu_in )
    ORDER BY
        FV.doc_num ASC;
END;
GO

-- 4. Actualizar las devoluciones existentes que tienen asignado N° N/CR (SERIE NC%)
--    pero campo5 contiene el Nro de Control o desalineado
UPDATE saDevolucionCliente
SET campo5 = LTRIM(RTRIM(nro_doc))
WHERE nro_doc LIKE 'SERIE NC%'
  AND (campo5 IS NULL OR LTRIM(RTRIM(campo5)) <> LTRIM(RTRIM(nro_doc)));
GO
