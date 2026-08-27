package com.metrowest.services.diagnostics;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Small helper for admin network troubleshooting. Runs a connectivity check
 * (ping) against a host and returns the raw command output so an admin can
 * confirm a customer site is reachable.
 */
@Service
public class DiagnosticsService
{
    public String pingHost(String host)
    {
        StringBuilder output = new StringBuilder();
        try
        {
            String command = "ping -c 1 " + host;
            Process process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command });

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
        }
        catch (Exception e)
        {
            output.append("diagnostics failed: ").append(e.getMessage());
        }
        return output.toString();
    }
}
