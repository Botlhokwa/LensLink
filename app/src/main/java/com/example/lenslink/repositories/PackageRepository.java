package com.example.lenslink.repositories;
import com.example.lenslink.models.Package;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class PackageRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final List<Package> packages = new ArrayList<>();
    static {
        packages.add(new Package("pkg1", "srv1", "Barred Production Studios", "Graduation Standard", 1500, 2, "90-min Studio & Outdoor Shoot, 25 High-Res Photos, 2 Outfits"));
        packages.add(new Package("pkg2", "srv1", "Barred Production Studios", "Graduation Premium", 2500, 3, "2-hr Session, 50 Photos, Family Portraits, Printed Frame"));
        packages.add(new Package("pkg3", "srv2", "Barred Production Studios", "Wedding Full-Day", 8500, 8, "Full Day Coverage, Drone Shots, 300+ Edited Photos, USB Delivery"));
        packages.add(new Package("pkg4", "srv3", "Kagiso Visuals", "Golden Hour Outdoor Session", 2500, 2, "35 High-Res Photos, Rosebank/Sandton Location, Sunset Lighting"));
    }

    public void addPackage(Package p, RepositoryCallback<Package> cb) {
        String id = "pkg_" + System.currentTimeMillis();
        p.setPackageId(id);
        packages.add(0, p);
        cb.onSuccess(p);
    }


    public void getPackagesForStudio(String studioName, RepositoryCallback<List<Package>> cb) {
        List<Package> res = new ArrayList<>();
        for (Package p : packages) {
            if (p.getPhotographerName() != null && p.getPhotographerName().equalsIgnoreCase(studioName)) res.add(p);
        }
        cb.onSuccess(res);
    }

    public void removePackage(String packageId, RepositoryCallback<Void> cb) {
        for (int i = 0; i < packages.size(); i++) {
            if (packages.get(i).getPackageId().equals(packageId)) { packages.remove(i); break; }
        }
        cb.onSuccess(null);
    }

    public void getAllPackages(RepositoryCallback<List<Package>> cb) {
        cb.onSuccess(new ArrayList<>(packages));
    }
}
