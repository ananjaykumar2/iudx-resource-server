package iudx.resource.server.apiserver.query;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.vertx.core.MultiMap;

/**
 * 
 * NGSILDQueryParams Class to parse query parameters from HTTP request.
 *
 */
public class NGSILDQueryParams {

	private List<URI> id;
	private List<String> type;
	private List<String> attrs;
	private String idPattern;
	private String q;
	private GeoRelation geoRel;
	private String geometry;
	private String coordinates;
	private String geoProperty;
	private TemporalRelation temporalRelation;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]'Z'")
			.withZone(ZoneId.systemDefault());;

	public NGSILDQueryParams(MultiMap paramsMap) {
		this.setGeoRel(new GeoRelation());
		this.setTemporalRelation(new TemporalRelation());
		this.create(paramsMap);
	}

	/**
	 * This method is used to initialize a NGSILDQueryParams object from multimap of
	 * query parameters.
	 * 
	 * @param paramsMap
	 */
	private void create(MultiMap paramsMap) {
		List<Entry<String, String>> entries = paramsMap.entries();

		for (final Entry<String, String> entry : entries) {
			switch (entry.getKey()) {
			case "id": {
				this.id = new ArrayList<URI>();
				String[] ids = entry.getValue().split(",");
				List<URI> uris = Arrays.stream(ids).map(e -> toURI(e)).collect(Collectors.toList());
				this.id.addAll(uris);
				break;
			}
			case "attrs": {
				this.attrs = new ArrayList<String>();
				this.attrs.addAll(Arrays.stream(entry.getValue().split(",")).collect(Collectors.toList()));
				break;
			}
			case "georel": {
				String georel = entry.getValue();
				String[] values = georel.split(";");
				this.geoRel.setRelation(values[0]);
				if (values.length == 2) {
					String[] distance = values[1].split("=");
					if (distance[0].equalsIgnoreCase("maxdistance")) {
						this.geoRel.setMaxDistance(Double.parseDouble(distance[1]));
					} else if (distance[0].equalsIgnoreCase("mindistance")) {
						this.geoRel.setMinDistance(Double.parseDouble(distance[1]));
					}
				}
				break;
			}
			case "geometry": {
				this.geometry = entry.getValue();
				break;
			}
			case "coordinates": {
				this.coordinates = entry.getValue();
				break;
			}
			case "timerel": {
				this.temporalRelation.setTemprel(entry.getValue());
				break;
			}
			case "time": {
				this.temporalRelation.setTime(LocalDateTime.parse(entry.getValue(), formatter));
				break;
			}
			case "endtime": {
				this.temporalRelation.setEndTime(LocalDateTime.parse(entry.getValue(), formatter));
				break;
			}
			case "q": {
				this.q = entry.getValue();
				break;
			}

			}
		}
	}

	private URI toURI(String source) {
		URI uri = null;
		try {
			uri = new URI(source);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return uri;
	}

	public List<URI> getId() {
		return id;
	}

	public void setId(List<URI> id) {
		this.id = id;
	}

	public List<String> getType() {
		return type;
	}

	public void setType(List<String> type) {
		this.type = type;
	}

	public List<String> getAttrs() {
		return attrs;
	}

	public void setAttrs(List<String> attrs) {
		this.attrs = attrs;
	}

	public String getIdPattern() {
		return idPattern;
	}

	public void setIdPattern(String idPattern) {
		this.idPattern = idPattern;
	}

	public String getQ() {
		return q;
	}

	public void setQ(String q) {
		this.q = q;
	}

	public GeoRelation getGeoRel() {
		return geoRel;
	}

	public void setGeoRel(GeoRelation geoRel) {
		this.geoRel = geoRel;
	}

	public String getGeometry() {
		return geometry;
	}

	public void setGeometry(String geometry) {
		this.geometry = geometry;
	}

	public String getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(String coordinates) {
		this.coordinates = coordinates;
	}

	public String getGeoProperty() {
		return geoProperty;
	}

	public void setGeoProperty(String geoProperty) {
		this.geoProperty = geoProperty;
	}

	public TemporalRelation getTemporalRelation() {
		return temporalRelation;
	}

	public void setTemporalRelation(TemporalRelation temporalRelation) {
		this.temporalRelation = temporalRelation;
	}

}
