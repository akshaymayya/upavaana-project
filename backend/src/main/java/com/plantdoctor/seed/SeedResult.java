package com.plantdoctor.seed;

import java.util.ArrayList;
import java.util.List;

public record SeedResult(
		String csvPath,
		int rowsRead,
		int plantsCreated,
		int diseasesInserted,
		int diseasesSkipped,
		List<String> errors) {

	public SeedResult {
		errors = errors != null ? List.copyOf(errors) : List.of();
	}

	public static SeedResult empty(String csvPath) {
		return new SeedResult(csvPath, 0, 0, 0, 0, List.of());
	}

	public static Builder builder(String csvPath) {
		return new Builder(csvPath);
	}

	public static final class Builder {

		private final String csvPath;
		private int rowsRead;
		private int plantsCreated;
		private int diseasesInserted;
		private int diseasesSkipped;
		private final List<String> errors = new ArrayList<>();

		private Builder(String csvPath) {
			this.csvPath = csvPath;
		}

		public Builder rowsRead(int rowsRead) {
			this.rowsRead = rowsRead;
			return this;
		}

		public Builder plantsCreated(int plantsCreated) {
			this.plantsCreated = plantsCreated;
			return this;
		}

		public Builder diseasesInserted(int diseasesInserted) {
			this.diseasesInserted = diseasesInserted;
			return this;
		}

		public Builder diseasesSkipped(int diseasesSkipped) {
			this.diseasesSkipped = diseasesSkipped;
			return this;
		}

		public Builder addError(String error) {
			this.errors.add(error);
			return this;
		}

		public SeedResult build() {
			return new SeedResult(csvPath, rowsRead, plantsCreated, diseasesInserted, diseasesSkipped, errors);
		}
	}
}
