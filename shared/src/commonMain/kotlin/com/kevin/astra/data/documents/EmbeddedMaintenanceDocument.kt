package com.kevin.astra.data.documents

import com.kevin.astra.domain.documents.AstraDocument
import com.kevin.astra.domain.documents.DocumentSection

object EmbeddedMaintenanceDocument {
    val industrialPumpMaintenanceGuide = AstraDocument(
        id = "industrial-pump-maintenance-guide",
        title = "Industrial Pump Maintenance Guide",
        sections = listOf(
            DocumentSection(
                title = "Overview",
                body = "Pump A is a critical centrifugal pump used to maintain process flow in the industrial cooling circuit. Operators must validate mechanical, electrical and pressure conditions before restarting the asset after any abnormal stop.",
            ),
            DocumentSection(
                title = "Safety Requirements",
                body = "Before intervention, confirm lockout/tagout status, release stored pressure only through approved valves, verify that the emergency stop circuit is reset, and keep the area clear of non-essential personnel.",
            ),
            DocumentSection(
                title = "Emergency Shutdown Procedure",
                body = "If vibration, overheating or pressure instability exceeds the operating threshold, press emergency stop, isolate the inlet valve, notify the shift supervisor, and record the alarm code from the local control panel.",
            ),
            DocumentSection(
                title = "Pump Restart Procedure",
                body = "To restart Pump A, confirm the emergency stop has been released, inspect the seal area, verify inlet pressure above 2.1 bar, reset the protection relay, select Local Mode, and start the pump from the control panel.",
            ),
            DocumentSection(
                title = "Pressure Monitoring",
                body = "After restart, monitor discharge pressure for five minutes. Normal operating pressure is between 3.2 and 4.1 bar. If pressure oscillates for more than 30 seconds, stop the pump and inspect the suction line.",
            ),
            DocumentSection(
                title = "Common Failure Symptoms",
                body = "Low inlet pressure may indicate a blocked strainer or closed valve. High vibration can indicate bearing wear or cavitation. Repeated relay trips usually indicate overload, wiring issues or motor thermal protection events.",
            ),
            DocumentSection(
                title = "Maintenance Checklist",
                body = "During each maintenance round, inspect coupling alignment, check bearing temperature, confirm lubrication level, clean the local panel, test alarm indicators, and document observations in the shift log.",
            ),
        ),
    )
}
