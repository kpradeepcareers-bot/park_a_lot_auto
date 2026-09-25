package com.kspcr.parkalot.auto

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates an offline, high-contrast, distraction-free HTML5/CSS3 dashboard
 * for Android Auto WebView rendering, matching the AABrowser architecture.
 */
object ParkALotAutoHtmlBuilder {

    fun buildParkingDashboardHtml(data: ParkingDisplayData): String {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val updateTime = timeFormat.format(Date(data.lastUpdated))

        val isTest = data.isTestData
        val hasOffices = data.officeName.isNotBlank() && data.officeName != "No Office Configured"
        val hasFloors = data.floors.isNotEmpty()

        val floorsHtml = StringBuilder()

        if (!hasFloors) {
            floorsHtml.append("""
                <div class="empty-state">
                    <div class="empty-icon">🅿️</div>
                    <div class="empty-title">No Parking Bays Configured</div>
                    <div class="empty-sub">Configure parking bays for <b>${escapeHtml(data.officeName)}</b> in the Park A Lot phone app.</div>
                </div>
            """.trimIndent())
        } else {
            for (floor in data.floors) {
                floorsHtml.append("""
                    <div class="floor-section">
                        <div class="floor-header">
                            <span class="floor-badge">${escapeHtml(floor.name)}</span>
                            <span class="floor-count">${floor.slots.size} bays</span>
                        </div>
                        <div class="slot-grid">
                """.trimIndent())

                for (slot in floor.slots) {
                    floorsHtml.append("""
                        <div class="slot-badge">${escapeHtml(slot)}</div>
                    """.trimIndent())
                }

                floorsHtml.append("""
                        </div>
                    </div>
                """.trimIndent())
            }
        }

        val testBannerHtml = if (isTest) {
            """<div class="test-banner">⚠️ DEVELOPER TEST DATA MODE (DELOITTE SAMPLE)</div>"""
        } else ""

        val arrivalStatusBadge = if (data.arrivalStatus) {
            """<span class="status-badge arrived">🟢 ARRIVED AT OFFICE</span>"""
        } else {
            """<span class="status-badge sync">🔄 LIVE SYNC</span>"""
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <title>Park A Lot Auto</title>
                <style>
                    * {
                        box-sizing: border-box;
                        margin: 0;
                        padding: 0;
                        -webkit-user-select: none;
                        user-select: none;
                    }
                    body {
                        background-color: #0B0F17;
                        color: #FFFFFF;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        padding: 16px 20px;
                        overflow-x: hidden;
                    }
                    .test-banner {
                        background-color: #F59E0B;
                        color: #000000;
                        font-weight: 800;
                        font-size: 14px;
                        text-align: center;
                        padding: 8px 12px;
                        border-radius: 8px;
                        margin-bottom: 12px;
                        letter-spacing: 0.5px;
                    }
                    .header-bar {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 2px solid #1E293B;
                        padding-bottom: 12px;
                        margin-bottom: 16px;
                    }
                    .app-title-group {
                        display: flex;
                        align-items: center;
                        gap: 10px;
                    }
                    .app-logo {
                        background: linear-gradient(135deg, #3B82F6, #1D4ED8);
                        color: #FFFFFF;
                        font-weight: 900;
                        font-size: 18px;
                        width: 36px;
                        height: 36px;
                        border-radius: 8px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .app-name {
                        font-size: 20px;
                        font-weight: 800;
                        letter-spacing: 0.5px;
                        color: #60A5FA;
                    }
                    .status-group {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    .status-badge {
                        font-size: 11px;
                        font-weight: 700;
                        padding: 4px 10px;
                        border-radius: 20px;
                        letter-spacing: 0.5px;
                    }
                    .status-badge.arrived {
                        background-color: rgba(16, 185, 129, 0.2);
                        color: #34D399;
                        border: 1px solid #10B981;
                    }
                    .status-badge.sync {
                        background-color: rgba(59, 130, 246, 0.2);
                        color: #93C5FD;
                        border: 1px solid #3B82F6;
                    }
                    .office-card {
                        background: #131B2A;
                        border: 1px solid #1E293B;
                        border-radius: 12px;
                        padding: 14px 18px;
                        margin-bottom: 16px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .office-name {
                        font-size: 26px;
                        font-weight: 800;
                        color: #FFFFFF;
                        letter-spacing: -0.5px;
                    }
                    .office-sub {
                        font-size: 13px;
                        color: #94A3B8;
                        margin-top: 2px;
                    }
                    .floor-section {
                        background: #111827;
                        border: 1px solid #1F2937;
                        border-radius: 14px;
                        padding: 14px;
                        margin-bottom: 14px;
                    }
                    .floor-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 12px;
                    }
                    .floor-badge {
                        background: #2563EB;
                        color: #FFFFFF;
                        font-size: 18px;
                        font-weight: 800;
                        padding: 4px 12px;
                        border-radius: 6px;
                        letter-spacing: 0.5px;
                    }
                    .floor-count {
                        font-size: 13px;
                        color: #94A3B8;
                        font-weight: 600;
                    }
                    .slot-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fill, minmax(76px, 1fr));
                        gap: 10px;
                    }
                    .slot-badge {
                        background: #1E293B;
                        border: 1px solid #334155;
                        color: #F8FAFC;
                        font-size: 20px;
                        font-weight: 700;
                        text-align: center;
                        padding: 10px 4px;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
                    }
                    .empty-state {
                        text-align: center;
                        padding: 40px 20px;
                        background: #131B2A;
                        border-radius: 14px;
                        border: 1px dashed #334155;
                    }
                    .empty-icon {
                        font-size: 44px;
                        margin-bottom: 12px;
                    }
                    .empty-title {
                        font-size: 20px;
                        font-weight: 700;
                        color: #E2E8F0;
                        margin-bottom: 6px;
                    }
                    .empty-sub {
                        font-size: 14px;
                        color: #94A3B8;
                    }
                    .footer {
                        text-align: center;
                        font-size: 11px;
                        color: #64748B;
                        margin-top: 16px;
                        padding-top: 12px;
                        border-top: 1px solid #1E293B;
                    }
                </style>
            </head>
            <body>
                $testBannerHtml
                <div class="header-bar">
                    <div class="app-title-group">
                        <div class="app-logo">P</div>
                        <div class="app-name">PARK A LOT AUTO</div>
                    </div>
                    <div class="status-group">
                        $arrivalStatusBadge
                    </div>
                </div>

                <div class="office-card">
                    <div>
                        <div class="office-name">${escapeHtml(data.officeName)}</div>
                        <div class="office-sub">${if (data.arrivalStatus) "Arrived at destination" else "Configured Office bays"}</div>
                    </div>
                </div>

                $floorsHtml

                <div class="footer">
                    Park A Lot • Last updated: $updateTime • Read-only Driver Display
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
