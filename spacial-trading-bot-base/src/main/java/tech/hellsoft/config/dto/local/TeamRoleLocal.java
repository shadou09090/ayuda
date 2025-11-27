package tech.hellsoft.config.dto.local;

import tech.hellsoft.trading.dto.server.TeamRole;

import java.io.Serial;
import java.io.Serializable;

/**
 * Versión serializable de {@link TeamRole} para guardar en snapshots.
 */
public final class TeamRoleLocal implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Integer branches;
  private final Integer maxDepth;
  private final Double decay;
  private final Double budget;
  private final Double baseEnergy;
  private final Double levelEnergy;

  private TeamRoleLocal(Integer branches, Integer maxDepth, Double decay, Double budget, Double baseEnergy,
      Double levelEnergy) {
    this.branches = branches;
    this.maxDepth = maxDepth;
    this.decay = decay;
    this.budget = budget;
    this.baseEnergy = baseEnergy;
    this.levelEnergy = levelEnergy;
  }

  public static TeamRoleLocal from(TeamRole rol) {
    if (rol == null) {
      return null;
    }
    return new TeamRoleLocal(rol.getBranches(), rol.getMaxDepth(), rol.getDecay(), rol.getBudget(), rol.getBaseEnergy(),
        rol.getLevelEnergy());
  }

  public TeamRole toTeamRole() {
    TeamRole.TeamRoleBuilder builder = TeamRole.builder();
    builder.branches(branches);
    builder.maxDepth(maxDepth);
    builder.decay(decay);
    builder.budget(budget);
    builder.baseEnergy(baseEnergy);
    builder.levelEnergy(levelEnergy);
    return builder.build();
  }
}

