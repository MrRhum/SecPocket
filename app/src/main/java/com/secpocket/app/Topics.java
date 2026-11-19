package com.secpocket.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The controlled vocabulary used by the catalog and the UI.
 */
public final class Topics {
    private static final List<Topic> ALL = Collections.unmodifiableList(Arrays.asList(
            new Topic("Governance & Risk", "Governance", 0xFF7C9CFF, R.drawable.ic_topic_governance,
                    "Strategy, risk, controls and assurance."),
            new Topic("Cloud", "Cloud", 0xFF38BDF8, R.drawable.ic_topic_cloud,
                    "Shared responsibility, zero trust and cloud assurance."),
            new Topic("AI & Emerging Tech", "AI", 0xFFC084FC, R.drawable.ic_topic_ai,
                    "AI governance, adversarial ML and generative AI security."),
            new Topic("Application Security", "AppSec", 0xFF4FD1C5, R.drawable.ic_topic_appsec,
                    "Secure development, verification and software risk."),
            new Topic("Identity & Access", "Identity", 0xFFFBBF24, R.drawable.ic_topic_identity,
                    "Authentication, authorization and digital identity."),
            new Topic("Network, Wi-Fi & PAN", "Network & PAN", 0xFF4ADE80, R.drawable.ic_topic_network,
                    "Wi-Fi, Bluetooth, 5G and personal-area networks."),
            new Topic("Data & Privacy", "Privacy", 0xFFF472B6, R.drawable.ic_topic_privacy,
                    "Personal data, privacy risk and data lifecycle."),
            new Topic("Payments & PAN", "Payments", 0xFFF59E0B, R.drawable.ic_topic_payments,
                    "Cardholder data, PAN protection and financial controls."),
            new Topic("OT / ICS", "OT / ICS", 0xFFFB923C, R.drawable.ic_topic_ot,
                    "Industrial control, safety and cyber-physical systems."),
            new Topic("Incident Response & Threat Intel", "Incident", 0xFFF87171, R.drawable.ic_topic_ir,
                    "Response, forensics and adversary-informed defense."),
            new Topic("Endpoint & Mobility", "Mobility", 0xFF9CA3AF, R.drawable.ic_topic_mobility,
                    "Mobile apps, devices and enterprise mobility."),
            new Topic("Supply Chain & Product Security", "Supply chain", 0xFFA3E635, R.drawable.ic_topic_supply,
                    "Suppliers, provenance, SBOM and product assurance."),
            new Topic("Resilience & Regulation", "Regulation", 0xFFE879F9, R.drawable.ic_topic_regulation,
                    "Regulation, continuity and operational resilience."),
            new Topic("Cryptography", "Crypto", 0xFF67E8F9, R.drawable.ic_topic_crypto,
                    "Cryptographic standards, key management and PQC.")
    ));

    private static final Map<String, Topic> BY_NAME;

    static {
        Map<String, Topic> map = new LinkedHashMap<>();
        for (Topic topic : ALL) {
            map.put(topic.name, topic);
        }
        BY_NAME = Collections.unmodifiableMap(map);
    }

    private Topics() {
    }

    public static List<Topic> all() {
        return ALL;
    }

    public static Topic byName(String name) {
        Topic topic = BY_NAME.get(name);
        if (topic != null) {
            return topic;
        }
        return new Topic(name, name, 0xFF4FD1C5, R.drawable.ic_topic_governance,
                "Cybersecurity guidance.");
    }

    public static int colorFor(String name) {
        return byName(name).color;
    }

    public static int iconFor(String name) {
        return byName(name).iconRes;
    }

    public static String shortLabel(String name) {
        return byName(name).shortName;
    }
}
