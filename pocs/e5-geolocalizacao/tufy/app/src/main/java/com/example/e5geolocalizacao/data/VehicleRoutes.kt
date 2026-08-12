package com.example.e5geolocalizacao.data

object VehicleRoutes {

    val caminhao01 = VehicleInfo(id = "V-001", label = "Caminhão 01")
    val furgao02 = VehicleInfo(id = "V-002", label = "Furgão 02")

    val rotaCaminhao01: List<RoutePoint> = listOf(
        RoutePoint(VehiclePosition(-21.7945, -48.1756), Telemetry(0.0, false, EngineState.DESLIGADO)),
        RoutePoint(VehiclePosition(-21.7945, -48.1756), Telemetry(0.0, true, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7930, -48.1740), Telemetry(18.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7905, -48.1720), Telemetry(32.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7875, -48.1730), Telemetry(41.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7845, -48.1761), Telemetry(35.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7820, -48.1790), Telemetry(0.0, true, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7820, -48.1790), Telemetry(0.0, true, EngineState.DESLIGADO)),
        RoutePoint(VehiclePosition(-21.7820, -48.1790), Telemetry(0.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7795, -48.1810), Telemetry(28.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7765, -48.1825), Telemetry(37.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7735, -48.1840), Telemetry(20.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7715, -48.1855), Telemetry(0.0, true, EngineState.LIGADO))
    )

    val rotaFurgao02: List<RoutePoint> = listOf(
        RoutePoint(VehiclePosition(-21.7995, -48.1690), Telemetry(0.0, true, EngineState.DESLIGADO)),
        RoutePoint(VehiclePosition(-21.7995, -48.1690), Telemetry(0.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7975, -48.1705), Telemetry(22.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7945, -48.1715), Telemetry(30.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7910, -48.1725), Telemetry(15.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7910, -48.1725), Telemetry(0.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7880, -48.1745), Telemetry(26.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7850, -48.1765), Telemetry(33.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7825, -48.1780), Telemetry(19.0, false, EngineState.LIGADO)),
        RoutePoint(VehiclePosition(-21.7800, -48.1800), Telemetry(0.0, true, EngineState.LIGADO))
    )

    val fleet: Map<VehicleInfo, List<RoutePoint>> = mapOf(
        caminhao01 to rotaCaminhao01,
        furgao02 to rotaFurgao02
    )
}
