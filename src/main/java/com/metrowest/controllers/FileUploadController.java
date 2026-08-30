package com.metrowest.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Controller
public class FileUploadController
{
    @PostMapping("/admin/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model)
    {
        try
        {
            String uploadDir = "/var/www/uploads/";
            String filename = file.getOriginalFilename();
            File dest = new File(uploadDir + filename);

            dest.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(dest);
            fos.write(file.getBytes());
            fos.close();

            model.addAttribute("message", "File uploaded: " + filename);
            model.addAttribute("path", dest.getAbsolutePath());
        }
        catch (IOException e)
        {
            model.addAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "admin/upload_result";
    }

    @PostMapping("/admin/delete_file")
    public String deleteFile(@RequestParam("filename") String filename, Model model)
    {
        try
        {
            File file = new File(filename);

            if (file.exists())
            {
                file.delete();
                model.addAttribute("message", "File deleted: " + filename);
            }
            else
            {
                model.addAttribute("error", "File not found: " + filename);
            }
        }
        catch (Exception e)
        {
            model.addAttribute("error", "Deletion failed: " + e.getMessage());
        }
        return "admin/delete_result";
    }

    @PostMapping("/admin/list_directory")
    public String listDirectory(@RequestParam("path") String path, Model model)
    {
        try
        {
            File dir = new File(path);
            File[] files = dir.listFiles();

            if (files != null)
            {
                StringBuilder listing = new StringBuilder();
                for (File f : files)
                {
                    listing.append(f.getAbsolutePath()).append("\n");
                }
                model.addAttribute("listing", listing.toString());
            }
        }
        catch (Exception e)
        {
            model.addAttribute("error", "Directory listing failed: " + e.getMessage());
        }
        return "admin/directory_listing";
    }

    @PostMapping("/admin/read_file")
    public String readFile(@RequestParam("filepath") String filepath, Model model)
    {
        try
        {
            String content = new String(Files.readAllBytes(Paths.get(filepath)));
            model.addAttribute("content", content);
            model.addAttribute("filepath", filepath);
        }
        catch (IOException e)
        {
            model.addAttribute("error", "Failed to read file: " + e.getMessage());
        }
        return "admin/file_content";
    }
}
