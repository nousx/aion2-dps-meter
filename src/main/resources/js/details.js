const createDetailsUI = ({
  detailsPanel,
  detailsClose,
  detailsTitle,
  detailsStatsEl,
  skillsListEl,
  dpsFormatter,
  getDetails,
  formatNumber,
}) => {
  let openedRowId = null;
  let openSeq = 0;
  let lastRow = null;
  let lastDetails = null;
  let refreshDebounceTimer = null;

  const clamp01 = (v) => Math.max(0, Math.min(1, v));

  const formatNum = (v) => {
    const n = Number(v);
    if (!Number.isFinite(n)) return "-";
    return formatNumber ? formatNumber(n) : dpsFormatter.format(n);
  };
  const pctText = (v) => {
    const n = Number(v);
    return Number.isFinite(n) ? `${n.toFixed(1)}%` : "-";
  };
  const i18n = window.i18n;
  const labelText = (key, fallback) => i18n?.t?.(key, fallback) ?? fallback;
  const formatTitle = (name) =>
    i18n?.format?.("details.title", { name }, `${name} Details`) ?? `${name} Details`;

  const STATUS = [
    { key: "details.stats.damage", fallback: "Damage", getValue: (d) => formatNum(d?.totalDmg), className: "stat-damage" },
    { key: "details.stats.dps", fallback: "DPS", getValue: (d) => {
      const dps = Number(d?.dps) || 0;
      return formatNum(dps); // DPS already means "per second"
    }, className: "stat-dps" },
    { key: "details.stats.contribution", fallback: "Contribution", getValue: (d) => pctText(d?.contributionPct), className: "stat-contribution" },
    { key: "details.stats.critRate", fallback: "Crit Rate", getValue: (d) => pctText(d?.totalCritPct), className: "stat-crit" },
    { key: "details.stats.perfectRate", fallback: "Perfect Rate", getValue: (d) => pctText(d?.totalPerfectPct), className: "stat-perfect" },
    { key: "details.stats.doubleRate", fallback: "Double Rate", getValue: (d) => pctText(d?.totalDoublePct), className: "stat-double" },
    { key: "details.stats.backRate", fallback: "Back Attack Rate", getValue: (d) => pctText(d?.totalBackPct), className: "stat-back" },
    { key: "details.stats.parryRate", fallback: "Parry Rate", getValue: (d) => pctText(d?.totalParryPct), className: "stat-parry" },
    { key: "details.stats.combatTime", fallback: "Combat Time", getValue: (d) => d?.combatTime ?? "-", className: "stat-time" },
  ];

  const createStatView = (labelKey, fallbackLabel, className) => {
    const statEl = document.createElement("div");
    statEl.className = `stat ${className || ''}`;

    const labelEl = document.createElement("p");
    labelEl.className = "label";
    labelEl.textContent = labelText(labelKey, fallbackLabel);

    const valueEl = document.createElement("p");
    valueEl.className = "value";
    valueEl.textContent = "-";

    statEl.appendChild(labelEl);
    statEl.appendChild(valueEl);

    return { statEl, labelEl, valueEl, labelKey, fallbackLabel };
  };

  const statSlots = STATUS.map((def) => createStatView(def.key, def.fallback, def.className));
  statSlots.forEach((value) => detailsStatsEl.appendChild(value.statEl));

  const updateLabels = () => {
    for (let i = 0; i < statSlots.length; i++) {
      const slot = statSlots[i];
      slot.labelEl.textContent = labelText(slot.labelKey, slot.fallbackLabel);
    }
    if (!detailsPanel.classList.contains("open")) {
      detailsTitle.textContent = labelText("details.header", "Details");
    } else if (currentRowName) {
      detailsTitle.textContent = formatTitle(currentRowName);
    }
  };

  const renderStats = (details) => {
    for (let i = 0; i < STATUS.length; i++) {
      statSlots[i].valueEl.textContent = STATUS[i].getValue(details);
    }
  };

  const createSkillView = () => {
    const rowEl = document.createElement("div");
    rowEl.className = "skillRow";

    // Main row container (name, hit, avg, dmg)
    const mainRowEl = document.createElement("div");
    mainRowEl.className = "skillMainRow";

    const nameEl = document.createElement("div");
    nameEl.className = "cell name clickable";
    nameEl.style.cursor = "pointer";

    const iconEl = document.createElement("img");
    iconEl.className = "skillIcon";
    iconEl.style.width = "24px";
    iconEl.style.height = "24px";
    iconEl.style.marginRight = "6px";
    iconEl.style.borderRadius = "4px";
    iconEl.onerror = () => {
      // Prevent infinite loop if noimg.png also doesn't exist
      if (!iconEl.src.endsWith('noimg.png')) {
        iconEl.src = "assets/noimg.png";
      }
    };

    const nameTextEl = document.createElement("span");
    nameTextEl.className = "skillNameText";

    nameEl.appendChild(iconEl);
    nameEl.appendChild(nameTextEl);

    const hitEl = document.createElement("div");
    hitEl.className = "cell hit";

    const avgEl = document.createElement("div");
    avgEl.className = "cell avg";

    const dmgEl = document.createElement("div");
    dmgEl.className = "cell dmg";

    const dmgFillEl = document.createElement("div");
    dmgFillEl.className = "dmgFill";

    const dmgTextEl = document.createElement("div");
    dmgTextEl.className = "dmgText";

    dmgEl.appendChild(dmgFillEl);
    dmgEl.appendChild(dmgTextEl);

    mainRowEl.appendChild(nameEl);
    mainRowEl.appendChild(hitEl);
    mainRowEl.appendChild(avgEl);
    mainRowEl.appendChild(dmgEl);

    // Specialty + stats container (collapsible)
    const expandEl = document.createElement("div");
    expandEl.className = "skillExpand";
    expandEl.style.display = "none";

    // Specialty slots container
    const specialtyEl = document.createElement("div");
    specialtyEl.className = "specialtySlots";

    const specialtyLabel = document.createElement("span");
    specialtyLabel.className = "specialtyLabel";
    specialtyLabel.textContent = "Specialty: ";

    const slotsContainer = document.createElement("div");
    slotsContainer.className = "slots";

    // Create 5 slots
    const slotEls = [];
    for (let i = 1; i <= 5; i++) {
      const slot = document.createElement("div");
      slot.className = "slot";
      slot.textContent = i;
      slotEls.push(slot);
      slotsContainer.appendChild(slot);
    }

    specialtyEl.appendChild(specialtyLabel);
    specialtyEl.appendChild(slotsContainer);

    // Stats container
    const statsEl = document.createElement("div");
    statsEl.className = "skillStats";

    const critEl = document.createElement("div");
    critEl.className = "stat";
    const parryEl = document.createElement("div");
    parryEl.className = "stat";
    const perfectEl = document.createElement("div");
    perfectEl.className = "stat";
    const doubleEl = document.createElement("div");
    doubleEl.className = "stat";
    const backEl = document.createElement("div");
    backEl.className = "stat";

    statsEl.appendChild(critEl);
    statsEl.appendChild(parryEl);
    statsEl.appendChild(perfectEl);
    statsEl.appendChild(doubleEl);
    statsEl.appendChild(backEl);

    expandEl.appendChild(specialtyEl);
    expandEl.appendChild(statsEl);

    rowEl.appendChild(mainRowEl);
    rowEl.appendChild(expandEl);

    return {
      rowEl,
      nameEl,
      iconEl,
      nameTextEl,
      hitEl,
      avgEl,
      critEl,
      parryEl,
      backEl,
      perfectEl,
      doubleEl,
      dmgFillEl,
      dmgTextEl,
      expandEl,
      slotEls,
      isExpanded: false,
    };
  };

  const skillSlots = [];
  const ensureSkillSlots = (n) => {
    while (skillSlots.length < n) {
      const v = createSkillView();
      skillSlots.push(v);
      skillsListEl.appendChild(v.rowEl);
    }
  };

  const renderSkills = (details) => {
    const skills = Array.isArray(details?.skills) ? details.skills : [];
    const topSkills = [...skills].sort((a, b) => (Number(b?.dmg) || 0) - (Number(a?.dmg) || 0));
    // .slice(0, 12);

    const totalDamage = Number(details?.totalDmg);
    if (!Number.isFinite(totalDamage) || totalDamage <= 0) {
      // uiDebug?.log("details:invalidTotalDmg", details);
      return;
    }
    const percentBaseTotal = totalDamage;

    ensureSkillSlots(topSkills.length);

    for (let i = 0; i < skillSlots.length; i++) {
      const view = skillSlots[i];
      const skill = topSkills[i];

      if (!skill) {
        view.rowEl.style.display = "none";
        view.dmgFillEl.style.transform = "scaleX(0)";
        continue;
      }

      view.rowEl.style.display = "";

      const damage = skill.dmg || 0;
      const barFillRatio = clamp01(damage / percentBaseTotal);
      const hits = skill.time || 0;
      const crits = skill.crit || 0;
      const parry = skill.parry || 0;
      const perfect = skill.perfect || 0;
      const double = skill.double || 0;
      const back = skill.back || 0;

      const pct = (num, den) => (den > 0 ? Math.round((num / den) * 100) : 0);

      const damageRate = percentBaseTotal > 0 ? (damage / percentBaseTotal) * 100 : 0;
      const avgDamage = hits > 0 ? damage / hits : 0;

      const critRate = pct(crits, hits);
      const parryRate = pct(parry, hits);
      const backRate = pct(back, hits);
      const perfectRate = pct(perfect, hits);
      const doubleRate = pct(double, hits);

      // Set skill icon
      const skillCode = skill.code || "";
      view.iconEl.src = `assets/skills/${skillCode}.png`;

      view.nameTextEl.textContent = skill.name ?? "";
      view.hitEl.textContent = `${hits}`;
      view.avgEl.textContent = formatNum(avgDamage);
      view.dmgTextEl.textContent = `${formatNum(damage)} (${damageRate.toFixed(1)}%)`;
      view.dmgFillEl.style.transform = `scaleX(${barFillRatio})`;

      // Toggle expand on name click
      view.nameEl.onclick = () => {
        view.isExpanded = !view.isExpanded;
        view.expandEl.style.display = view.isExpanded ? "flex" : "none";
      };

      // Render specialty slots
      const specialtySlots = Array.isArray(skill.specialtySlots) ? skill.specialtySlots : [];

      // Debug logging - write to DOM
      if (i === 0) {
        const debugDiv = document.createElement('div');
        debugDiv.style.cssText = 'position: fixed; top: 10px; right: 10px; background: black; color: lime; padding: 10px; z-index: 9999; font-size: 10px; max-width: 300px; white-space: pre-wrap;';
        debugDiv.textContent = `DEBUG (First Skill):
Name: ${skill.name}
specialtySlots: ${JSON.stringify(skill.specialtySlots)}
Type: ${typeof skill.specialtySlots}
isArray: ${Array.isArray(skill.specialtySlots)}
Parsed: ${JSON.stringify(specialtySlots)}
slotEls: ${view.slotEls?.length || 'undefined'}`;
        document.body.appendChild(debugDiv);
        setTimeout(() => debugDiv.remove(), 10000);
      }

      view.slotEls?.forEach((slotEl, idx) => {
        const slotNum = idx + 1;
        if (specialtySlots.includes(slotNum)) {
          slotEl.classList.add("active");
        } else {
          slotEl.classList.remove("active");
        }
      });

      // Render stats in expanded section
      view.critEl.textContent = `Crit: ${critRate}%`;
      view.parryEl.textContent = `Parry: ${parryRate}%`;
      view.perfectEl.textContent = `Perfect: ${perfectRate}%`;
      view.doubleEl.textContent = `Double: ${doubleRate}%`;
      view.backEl.textContent = `Back: ${backRate}%`;
    }
  };

  let currentRowName = "";

  const render = (details, row) => {
    currentRowName = String(row.name);
    detailsTitle.textContent = formatTitle(currentRowName);
    renderStats(details);
    renderSkills(details);
    lastRow = row;
    lastDetails = details;
  };

  const isOpen = () => detailsPanel.classList.contains("open");

  const open = async (row, { force = false, restartOnSwitch = true } = {}) => {
    const rowId = row?.id ?? null;
    // if (!rowId) return;

    const isOpen = detailsPanel.classList.contains("open");
    const isSame = isOpen && openedRowId === rowId;
    const isSwitch = isOpen && openedRowId && openedRowId !== rowId;

    if (!force && isSame) return;

    if (isSwitch && restartOnSwitch) {
      close();
      requestAnimationFrame(() => {
        open(row, { force: true, restartOnSwitch: false });
      });
      return;
    }

    openedRowId = rowId;
    lastRow = row;

    currentRowName = String(row.name);
    detailsTitle.textContent = formatTitle(currentRowName);
    detailsPanel.classList.add("open");

    // Hide debug console when details panel opens
    const debugConsole = document.getElementById('debugConsole');
    if (debugConsole) {
      debugConsole.style.display = 'none';
    }

    // 이전 값 비우기
    for (let i = 0; i < statSlots.length; i++) statSlots[i].valueEl.textContent = "-";
    for (let i = 0; i < skillSlots.length; i++) {
      skillSlots[i].rowEl.style.display = "none";
      skillSlots[i].dmgFillEl.style.transform = "scaleX(0)";
    }

    const seq = ++openSeq;

    try {
      const details = await getDetails(row);

      if (seq !== openSeq) return;

      render(details, row);
    } catch (e) {
      if (seq !== openSeq) return;
      // uiDebug?.log("getDetails:error", { id: rowId, message: e?.message });
    }
  };
  const close = () => {
    openSeq++;

    openedRowId = null;
    lastRow = null;
    lastDetails = null;
    detailsPanel.classList.remove("open");
  };
  detailsClose?.addEventListener("click", close);

  const refresh = () => {
    if (!detailsPanel.classList.contains("open") || !lastRow) {
      return;
    }

    // อัพเดทแบบ in-place โดยไม่ clear (ป้องกันกระพริบ)
    const seq = ++openSeq;

    getDetails(lastRow)
      .then(details => {
        if (seq !== openSeq) return; // ยกเลิกถ้ามี refresh ใหม่
        if (!detailsPanel.classList.contains("open")) return; // ปิดแล้ว

        // อัพเดทโดยตรง ไม่ล้าง
        render(details, lastRow);
      })
      .catch((err) => {
        console.error("Details refresh error:", err);
      });
  };

  const performRefresh = refresh; // Alias for backward compatibility

  return {
    open,
    close,
    isOpen,
    render,
    updateLabels,
    refresh,
    get lastRow() { return lastRow; },
    set lastRow(value) { lastRow = value; }
  };
};
