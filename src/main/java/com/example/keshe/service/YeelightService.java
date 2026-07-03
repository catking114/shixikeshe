package com.example.keshe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class YeelightService {

    private static final Logger log = LoggerFactory.getLogger(YeelightService.class);
    private static final int DEFAULT_PORT = 55443;
    private static final int TIMEOUT_MS = 3000;
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AtomicLong commandId = new AtomicLong(1);

    public Map<String, Object> turnOn(String ip, int port) {
        return sendCommand(ip, port, "set_power", new Object[]{"on", "smooth", 500});
    }

    public Map<String, Object> turnOff(String ip, int port) {
        return sendCommand(ip, port, "set_power", new Object[]{"off", "smooth", 500});
    }

    public Map<String, Object> toggle(String ip, int port) {
        return sendCommand(ip, port, "toggle", new Object[]{});
    }

    public Map<String, Object> setBrightness(String ip, int port, int brightness) {
        brightness = Math.max(1, Math.min(100, brightness));
        return sendCommand(ip, port, "set_bright", new Object[]{brightness, "smooth", 500});
    }

    public Map<String, Object> setColorTemp(String ip, int port, int colorTemp) {
        colorTemp = Math.max(2700, Math.min(6500, colorTemp));
        return sendCommand(ip, port, "set_ct_abx", new Object[]{colorTemp, "smooth", 500});
    }

    public Map<String, Object> setRGB(String ip, int port, int r, int g, int b) {
        int rgbValue = (r * 65536) + (g * 256) + b;
        return sendCommand(ip, port, "set_rgb", new Object[]{rgbValue, "smooth", 500});
    }

    public Map<String, Object> setHSV(String ip, int port, int hue, int sat) {
        hue = Math.max(0, Math.min(359, hue));
        sat = Math.max(0, Math.min(100, sat));
        return sendCommand(ip, port, "set_hsv", new Object[]{hue, sat, "smooth", 500});
    }

    public Map<String, Object> getProps(String ip, int port) {
        String[] props = {"power", "bright", "ct", "rgb", "hue", "sat", "color_mode", "name"};
        return sendCommand(ip, port, "get_prop", new Object[]{props});
    }

    public Map<String, Object> setName(String ip, int port, String name) {
        return sendCommand(ip, port, "set_name", new Object[]{name});
    }

    public List<Map<String, Object>> discoverDevices(int timeoutSeconds) {
        List<Map<String, Object>> devices = new ArrayList<>();
        String ssdpMulticast = "239.255.255.250";
        int ssdpPort = 1982;
        String searchMessage = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: " + ssdpMulticast + ":" + ssdpPort + "\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "ST: wifi_bulb\r\n\r\n";

        try (MulticastSocket socket = new MulticastSocket()) {
            socket.setSoTimeout(timeoutSeconds * 1000);
            InetAddress multicastAddress = InetAddress.getByName(ssdpMulticast);
            byte[] sendData = searchMessage.getBytes(StandardCharsets.UTF_8);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, multicastAddress, ssdpPort);
            socket.send(sendPacket);

            byte[] receiveBuffer = new byte[4096];
            long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

            while (System.currentTimeMillis() < endTime) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
                    Map<String, Object> device = parseSsdpResponse(response);
                    if (device != null && !devices.stream().anyMatch(d -> d.get("Location").equals(device.get("Location")))) {
                        devices.add(device);
                        log.info("Discovered Yeelight device: {}", device);
                    }
                } catch (SocketTimeoutException e) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("SSDP discovery failed", e);
        }
        return devices;
    }

    private Map<String, Object> sendCommand(String ip, int port, String method, Object[] params) {
        if (port <= 0) port = DEFAULT_PORT;
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("id", commandId.getAndIncrement());
        command.put("method", method);
        command.put("params", params);

        String jsonCommand;
        try {
            jsonCommand = mapper.writeValueAsString(command) + "\r\n";
        } catch (Exception e) {
            return errorResult("Serialize failed: " + e.getMessage());
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            out.write(jsonCommand.getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String responseLine = reader.readLine();

            if (responseLine != null) {
                JsonNode response = mapper.readTree(responseLine);
                Map<String, Object> result = new LinkedHashMap<>();
                if (response.has("error")) {
                    result.put("success", false);
                    result.put("error", response.get("error").toString());
                } else {
                    result.put("success", true);
                    if (response.has("result")) {
                        result.put("result", mapper.treeToValue(response.get("result"), Object.class));
                    }
                }
                return result;
            }
            return errorResult("No response from device");
        } catch (SocketTimeoutException e) {
            return errorResult("Device response timeout");
        } catch (ConnectException e) {
            return errorResult("Cannot connect to device");
        } catch (Exception e) {
            return errorResult("Communication failed: " + e.getMessage());
        }
    }

    private Map<String, Object> parseSsdpResponse(String response) {
        Map<String, Object> device = new LinkedHashMap<>();
        for (String line : response.split("\r\n")) {
            if (line.startsWith("Location:")) device.put("Location", line.substring(9).trim());
            else if (line.startsWith("id:")) device.put("id", line.substring(3).trim());
            else if (line.startsWith("model:")) device.put("model", line.substring(6).trim());
            else if (line.startsWith("support:")) device.put("support", line.substring(8).trim());
            else if (line.startsWith("power:")) device.put("power", line.substring(6).trim());
            else if (line.startsWith("bright:")) device.put("bright", line.substring(7).trim());
            else if (line.startsWith("color_mode:")) device.put("color_mode", line.substring(11).trim());
            else if (line.startsWith("ct:")) device.put("ct", line.substring(3).trim());
            else if (line.startsWith("rgb:")) device.put("rgb", line.substring(4).trim());
            else if (line.startsWith("hue:")) device.put("hue", line.substring(4).trim());
            else if (line.startsWith("sat:")) device.put("sat", line.substring(4).trim());
            else if (line.startsWith("name:")) device.put("name", line.substring(5).trim());
        }
        if (!device.containsKey("Location")) return null;

        String location = (String) device.get("Location");
        if (location.startsWith("yeelight://")) {
            String hostPort = location.substring(11);
            String[] parts = hostPort.split(":");
            if (parts.length == 2) {
                device.put("ip", parts[0]);
                try { device.put("port", Integer.parseInt(parts[1])); }
                catch (NumberFormatException e) { device.put("port", DEFAULT_PORT); }
            }
        }
        return device;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", message);
        return result;
    }
}
