package geo.master.geopackagemap;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.core.contents.ContentsDao;
import mil.nga.geopackage.core.srs.SpatialReferenceSystemDao;
import mil.nga.geopackage.extension.ExtensionsDao;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.columns.GeometryColumnsDao;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.index.FeatureIndexType;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlay;
import mil.nga.geopackage.map.tiles.overlay.GeoPackageOverlayFactory;
import mil.nga.geopackage.metadata.MetadataDao;
import mil.nga.geopackage.metadata.reference.MetadataReferenceDao;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.geopackage.schema.columns.DataColumnsDao;
import mil.nga.geopackage.schema.constraints.DataColumnConstraintsDao;
import mil.nga.geopackage.tiles.TileGenerator;
import mil.nga.geopackage.tiles.UrlTileGenerator;
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles;
import mil.nga.geopackage.tiles.features.FeatureTileGenerator;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile;
import mil.nga.geopackage.tiles.matrix.TileMatrixDao;
import mil.nga.geopackage.tiles.matrixset.TileMatrixSetDao;
import mil.nga.geopackage.tiles.user.TileCursor;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.wkb.geom.Geometry;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 99;
    File geoPackageFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void readDatabaseData() {
        File dir = Environment.getExternalStorageDirectory();
        geoPackageFile = new File(dir, "geoPackage" + File.separator + "cod.gpkg");

        GeoPackageManager manager = GeoPackageFactory.getManager(this);
        if (manager.databases().size() == 0) {
            boolean imported = manager.importGeoPackage(geoPackageFile);
        }
        List<String> databases = manager.databases();
        GeoPackage geoPackage = manager.open(databases.get(0));

//        List<String> features = geoPackage.getFeatureTables();
//
//        String featureTable = features.get(0);

        // GeoPackage Table DAOs
        SpatialReferenceSystemDao srsDao = geoPackage.getSpatialReferenceSystemDao();
        ContentsDao contentsDao = geoPackage.getContentsDao();
        GeometryColumnsDao geomColumnsDao = geoPackage.getGeometryColumnsDao();
        TileMatrixSetDao tileMatrixSetDao = geoPackage.getTileMatrixSetDao();
        TileMatrixDao tileMatrixDao = geoPackage.getTileMatrixDao();
        DataColumnsDao dataColumnsDao = geoPackage.getDataColumnsDao();
        DataColumnConstraintsDao dataColumnConstraintsDao = geoPackage.getDataColumnConstraintsDao();
        MetadataDao metadataDao = geoPackage.getMetadataDao();
        MetadataReferenceDao metadataReferenceDao = geoPackage.getMetadataReferenceDao();
        ExtensionsDao extensionsDao = geoPackage.getExtensionsDao();

        // Feature and tile tables
        List<String> features = geoPackage.getFeatureTables();
        List<String> tiles = geoPackage.getTileTables();

        // Query Features
        String featureTable = features.get(0);
        FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
        GoogleMapShapeConverter converter = new GoogleMapShapeConverter(
                featureDao.getProjection());
        FeatureCursor featureCursor = featureDao.queryForAll();
        try{
            while(featureCursor.moveToNext()){
                FeatureRow featureRow = featureCursor.getRow();
                GeoPackageGeometryData geometryData = featureRow.getGeometry();
                Geometry geometry = geometryData.getGeometry();
                GoogleMapShape shape = converter.toShape(geometry);
                GoogleMapShape mapShape = GoogleMapShapeConverter
                        .addShapeToMap(mMap, shape);
                Log.d("Cod postal:",featureRow.getValue("GEOCODIGO").toString());
                // ...
            }
        }finally{
            featureCursor.close();
        }

//// Query Tiles
//        String tileTable = tiles.get(0);
//        TileDao tileDao = geoPackage.getTileDao(tileTable);
//        TileCursor tileCursor = tileDao.queryForAll();
//        try{
//            while(tileCursor.moveToNext()){
//                TileRow tileRow = tileCursor.getRow();
//                byte[] tileBytes = tileRow.getTileData();
//                Bitmap tileBitmap = tileRow.getTileDataBitmap();
//                // ...
//            }
//        }finally{
//            tileCursor.close();
//        }
//
//
//
//// Tile Provider (GeoPackage or Google API)
//        TileProvider overlay = GeoPackageOverlayFactory
//                .getTileProvider(tileDao);
//        TileOverlayOptions overlayOptions = new TileOverlayOptions();
//        overlayOptions.tileProvider(overlay);
//        overlayOptions.zIndex(-1);
//        mMap.addTileOverlay(overlayOptions);
//
//// Index Features
//        FeatureIndexManager indexer = new FeatureIndexManager(getApplicationContext(), geoPackage, featureDao);
//        indexer.setIndexLocation(FeatureIndexType.GEOPACKAGE);
//        int indexedCount = indexer.index();
//
//        // Feature Tile Provider (dynamically draw tiles from features)
//        FeatureTiles featureTiles = new DefaultFeatureTiles(getApplicationContext(), featureDao);
//        featureTiles.setMaxFeaturesPerTile(10); // Set max features to draw per tile
//        NumberFeaturesTile numberFeaturesTile = new NumberFeaturesTile(this); // Custom feature tile implementation
//        featureTiles.setMaxFeaturesTileDraw(numberFeaturesTile); // Draw feature count tiles when max features passed
//        featureTiles.setIndexManager(indexer); // Set index manager to query feature indices
//        FeatureOverlay featureOverlay = new FeatureOverlay(featureTiles);
//        featureOverlay.setMinZoom(8); // Set zoom level to start showing tiles
//        TileOverlayOptions featureOverlayOptions = new TileOverlayOptions();
//        featureOverlayOptions.tileProvider(featureOverlay);
//        featureOverlayOptions.zIndex(-1); // Draw the feature tiles behind map markers
//        mMap.addTileOverlay(featureOverlayOptions);
//
//        BoundingBox boundingBox = new BoundingBox();
//        mil.nga.geopackage.projection.Projection projection = ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM);
//
//// URL Tile Generator (generate tiles from a URL)
//        TileGenerator urlTileGenerator = new UrlTileGenerator(getApplicationContext(), geoPackage,
//                "url_tile_table", "http://url/{z}/{x}/{y}.png", 2, 7, boundingBox, projection);
//        try {
//            int urlTileCount = urlTileGenerator.generateTiles();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

// Feature Tile Generator (generate tiles from features)
//        TileGenerator featureTileGenerator = new FeatureTileGenerator(this, geoPackage,
//                featureTable + "_GEOCODIGO", featureTiles, 10, 15, boundingBox, projection);
//
//        try {
//            int featureTileCount = featureTileGenerator.generateTiles();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

// Close database when done
        geoPackage.close();


//        FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
//        FeatureCursor featureCursor = featureDao.queryForAll();
//        try {
//            while (featureCursor.moveToNext()) {
//                FeatureRow featureRow = featureCursor.getRow();
//                GeoPackageGeometryData geometryData = featureRow.getGeometry();
//                Geometry geometry = geometryData.getGeometry();
//                Log.d("Cod postal: ", featureRow.getValue("GEOCODIGO").toString());
//                geometry.getGeometryType();
//            }
//        } finally {
//            featureCursor.close();
//        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                readDatabaseData();
            } else {
                checkLocationPermission();
            }
        } else {
            readDatabaseData();
        }

//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (android.support.v4.app.ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Permisos de lectura/escritura")
                        .setMessage("Esta aplicación necesita permisos de lectura y escritura, por favor acepta para un funcionamiento correcto.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                android.support.v4.app.ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                android.support.v4.app.ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {

                        readDatabaseData();
                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }

}
