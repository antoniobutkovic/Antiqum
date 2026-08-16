import ComposeApp
import CoreLocation
import GoogleMaps
import UIKit

enum GoogleMapsBootstrap {
    static func configure() {
        guard
            let apiKey = Bundle.main.object(forInfoDictionaryKey: "GoogleMapsApiKey") as? String,
            !apiKey.isEmpty,
            !apiKey.hasPrefix("YOUR_")
        else {
            return
        }

        GMSServices.provideAPIKey(apiKey)
        IosMuseumMapBridge.shared.factory = GoogleMuseumMapViewFactory()
    }
}

private final class GoogleMuseumMapViewFactory: NSObject, IosMuseumMapViewFactory {
    func makeMapView(
        onMuseumSelected: @escaping (String) -> Void,
        onUserLocationResolved: @escaping (MuseumMapLocation?) -> Void
    ) -> UIView {
        MuseumGoogleMapView(
            onMuseumSelected: onMuseumSelected,
            onUserLocationResolved: onUserLocationResolved
        )
    }

    func updateMapView(
        view: UIView,
        markers: [MuseumMapMarker],
        selectedMuseumId: String?,
        darkTheme: Bool,
        userLocation: MuseumMapLocation?,
        requestUserLocation: Bool,
        cameraRequestId: Int32
    ) {
        guard let mapView = view as? MuseumGoogleMapView else { return }
        mapView.update(
            markers: markers,
            selectedMuseumId: selectedMuseumId,
            darkTheme: darkTheme,
            userLocation: userLocation,
            requestUserLocation: requestUserLocation,
            cameraRequestId: cameraRequestId
        )
    }
}

private final class MuseumGoogleMapView: GMSMapView, GMSMapViewDelegate, CLLocationManagerDelegate {
    private static let world = CLLocationCoordinate2D(latitude: 20, longitude: 0)

    private let onMuseumSelected: (String) -> Void
    private let onUserLocationResolved: (MuseumMapLocation?) -> Void
    private let locationManager = CLLocationManager()
    private var markerSignature = ""
    private var lastCameraRequestId: Int32 = -1
    private var hasRequestedLocation = false

    init(
        onMuseumSelected: @escaping (String) -> Void,
        onUserLocationResolved: @escaping (MuseumMapLocation?) -> Void
    ) {
        self.onMuseumSelected = onMuseumSelected
        self.onUserLocationResolved = onUserLocationResolved
        let options = GMSMapViewOptions()
        options.camera = GMSCameraPosition.camera(withTarget: Self.world, zoom: 1.5)
        super.init(options: options)
        delegate = self
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        settings.compassButton = false
        settings.myLocationButton = false
        settings.zoomGestures = true
        settings.scrollGestures = true
        settings.rotateGestures = true
        settings.tiltGestures = true
        padding = UIEdgeInsets(top: 116, left: 0, bottom: 68, right: 0)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func update(
        markers: [MuseumMapMarker],
        selectedMuseumId: String?,
        darkTheme: Bool,
        userLocation: MuseumMapLocation?,
        requestUserLocation: Bool,
        cameraRequestId: Int32
    ) {
        overrideUserInterfaceStyle = darkTheme ? .dark : .light
        padding = UIEdgeInsets(top: 116, left: 0, bottom: selectedMuseumId == nil ? 68 : 178, right: 0)
        isMyLocationEnabled = userLocation != nil

        if requestUserLocation && !hasRequestedLocation {
            hasRequestedLocation = true
            requestLocation()
        }

        let signature = markers
            .map { "\($0.id):\($0.latitude):\($0.longitude)" }
            .joined(separator: "|") + ":selected=\(selectedMuseumId ?? ""):\(darkTheme)"
        if signature != markerSignature {
            markerSignature = signature
            clear()
            markers.forEach { museum in
                let selected = museum.id == selectedMuseumId
                let marker = GMSMarker(
                    position: CLLocationCoordinate2D(
                        latitude: museum.latitude,
                        longitude: museum.longitude
                    )
                )
                marker.title = museum.name
                marker.snippet = museum.category
                marker.userData = museum.id
                marker.icon = Self.museumMarkerIcon(selected: selected, darkTheme: darkTheme)
                marker.groundAnchor = CGPoint(x: 0.5, y: 0.5)
                marker.zIndex = selected ? 2 : 1
                marker.map = self
            }
        }

        if cameraRequestId != lastCameraRequestId {
            lastCameraRequestId = cameraRequestId
            let target: CLLocationCoordinate2D
            let zoom: Float
            if let userLocation {
                target = CLLocationCoordinate2D(
                    latitude: userLocation.latitude,
                    longitude: userLocation.longitude
                )
                zoom = 12
            } else {
                target = Self.world
                zoom = 1.5
            }
            animate(to: GMSCameraPosition.camera(withTarget: target, zoom: zoom))
        }
    }

    private func requestLocation() {
        switch locationManager.authorizationStatus {
        case .notDetermined:
            locationManager.requestWhenInUseAuthorization()
        case .authorizedAlways, .authorizedWhenInUse:
            locationManager.requestLocation()
        case .denied, .restricted:
            onUserLocationResolved(nil)
        @unknown default:
            onUserLocationResolved(nil)
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        guard hasRequestedLocation else { return }
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            manager.requestLocation()
        case .denied, .restricted:
            onUserLocationResolved(nil)
        case .notDetermined:
            break
        @unknown default:
            onUserLocationResolved(nil)
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else {
            onUserLocationResolved(nil)
            return
        }
        onUserLocationResolved(
            MuseumMapLocation(
                latitude: location.coordinate.latitude,
                longitude: location.coordinate.longitude
            )
        )
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        if (error as? CLError)?.code != .locationUnknown {
            onUserLocationResolved(nil)
        }
    }

    func mapView(_ mapView: GMSMapView, didTap marker: GMSMarker) -> Bool {
        guard let museumId = marker.userData as? String else { return false }
        onMuseumSelected(museumId)
        return true
    }

    private static func museumMarkerIcon(selected: Bool, darkTheme: Bool) -> UIImage {
        let size = selected ? CGSize(width: 50, height: 50) : CGSize(width: 42, height: 42)
        let fill = selected
            ? UIColor(red: 0.60, green: 0.42, blue: 0.26, alpha: 1)
            : darkTheme
                ? UIColor(red: 0.56, green: 0.70, blue: 0.65, alpha: 1)
                : UIColor(red: 0.13, green: 0.28, blue: 0.25, alpha: 1)

        return UIGraphicsImageRenderer(size: size).image { context in
            let bounds = CGRect(origin: .zero, size: size).insetBy(dx: 3, dy: 3)
            context.cgContext.setShadow(offset: CGSize(width: 0, height: 2), blur: 3, color: UIColor.black.withAlphaComponent(0.28).cgColor)
            fill.setFill()
            UIBezierPath(ovalIn: bounds).fill()
            context.cgContext.setShadow(offset: .zero, blur: 0, color: nil)

            if selected {
                UIColor.white.setStroke()
                let border = UIBezierPath(ovalIn: bounds.insetBy(dx: 1.25, dy: 1.25))
                border.lineWidth = 2.5
                border.stroke()
            }

            let symbolConfig = UIImage.SymbolConfiguration(pointSize: selected ? 20 : 17, weight: .bold)
            let symbol = UIImage(systemName: "building.columns.fill", withConfiguration: symbolConfig)?
                .withTintColor(.white, renderingMode: .alwaysOriginal)
            symbol?.draw(at: CGPoint(x: (size.width - (symbol?.size.width ?? 0)) / 2, y: (size.height - (symbol?.size.height ?? 0)) / 2))
        }
    }
}
