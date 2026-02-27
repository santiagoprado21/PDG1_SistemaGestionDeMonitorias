import './Reports.css';
import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import VerticalNavbar from './VerticalNavbar';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  Cell,
  LineChart, Line
} from 'recharts';
import {PopUp} from "./PopUp";
import { BACKEND_URL } from './config/ApiBackend';

function Reports() {

  const [monitorPerformanceDataOriginal, setMonitorPerformanceDataOriginal] = useState([]);
  const [professorDataOriginal, setProfessorDataOriginal] = useState([]);

  // console.log("Reports se está renderizando");
  const [asistenciaDataOriginal, setAsistenciaDataOriginal] = useState([]);

  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  //Pop up
  const [isOpen, setIsOpen] = useState(false)
  const [message, setMessage] = useState("")
  const [change, setChange] = useState(false)

  const [semester, setSemester] = useState('');
  const [program, setProgram] = useState('');
  const [course, setCourse] = useState('');
  const [professor, setProfessor] = useState('');
  const [monitor, setMonitor] = useState('');
  const [selectedMonitorRow, setSelectedMonitorRow] = useState('');
  const [monitorSortBy, setMonitorSortBy] = useState('name_asc');
  const [monitorPage, setMonitorPage] = useState(1);

  const loggedUserId = localStorage.getItem('userId');
  const loggedRole = (localStorage.getItem('role') || '').toLowerCase();

  const normalizeProfessorName = (name = '') => {
    const cleaned = String(name).replace(/^\s*dra?\.?\s+/i, '').trim();
    return cleaned || 'Profesor';
  };

  //Porcentaje efectividad por materia de monitores
  const[completedPercent, setCompletedPercent] = useState("")
  const[pendingPercent, setPendingPercent] = useState("")
  const[latePercent, setLatePercent] = useState("")
  const[porcentages, setPorcentages] = useState([{ completed: "0%", late: "0%", pending: "0%" }]);

  //Porcentaje efectividad por materia de profesores
  const[completedPercentProfessor, setCompletedPercentProfessor] = useState("")
  const[pendingPercentProfessor, setPendingPercentProfessor] = useState("")
  const[latePercentProfessor, setLatePercentProfessor] = useState("")
  const[porcentagesProfessor, setPorcentagesProfessor] = useState([{ completed: "0%", late: "0%", pending: "0%" }]);

  const fetchReportsData = useCallback(async () => {
    const user = localStorage.getItem('userId');
    const currentRole = (localStorage.getItem('role') || '').toLowerCase();
    const isProfessorRole = currentRole === 'professor';

    if (!user || !currentRole) {
      setMessage("No se pudo cargar la información: inicia sesión nuevamente para consultar reportes.");
      setIsOpen(true);
      setChange(true);
      return;
    }

    setIsRefreshing(true);
    try {
      const monitorResponse = await fetch(`${BACKEND_URL}/monitoring/getMonitorsReport/${user}/${currentRole}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': localStorage.getItem('token')
          },
      });
      const monitorJson = await monitorResponse.json();
      const monitorList = Array.isArray(monitorJson) ? monitorJson : [];
      const scopedMonitorList = isProfessorRole
        ? monitorList.filter((item) => String(item.idProfessor) === String(user))
        : monitorList;
      setMonitorPerformanceDataOriginal(scopedMonitorList);

      let professorList = [];
      if (isProfessorRole && user) {
        const professorResponse = await fetch(`${BACKEND_URL}/monitoring/getProfessorReport/${user}`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': localStorage.getItem('token')
          },
        });
        const professorJson = await professorResponse.json();
        professorList = Array.isArray(professorJson) ? professorJson : [];
      } else {
        const professorIds = [...new Set(scopedMonitorList.map(report => report.idProfessor).filter(Boolean))];
        for (const value of professorIds) {
          const professorResponse = await fetch(`${BACKEND_URL}/monitoring/getProfessorReport/${value}`,{
              method: 'GET',
              headers: { 'Content-Type': 'application/json' ,
                  'Authorization':localStorage.getItem('token')
              },
          });
          const professorJson = await professorResponse.json();
          professorList = professorList.concat(Array.isArray(professorJson) ? professorJson : []);
        }
      }
      setProfessorDataOriginal(professorList);

      const attendanceResponse = await fetch(`${BACKEND_URL}/monitoring/getAttendanceReport/${currentRole}/${user}`,{
          method: 'GET',
          headers: { 'Content-Type': 'application/json' ,
              'Authorization':localStorage.getItem('token')
          },
          });
      const attendanceJson = await attendanceResponse.json();
      setAsistenciaDataOriginal(Array.isArray(attendanceJson) ? attendanceJson : []);
      setLastUpdatedAt(new Date());
    } catch (error) {
      console.error("Error actualizando reportes en tiempo real:", error);
    } finally {
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    setMessage("Usa los filtros de período, departamento, profesor o monitor para consultar el dashboard de reportes.")
    setIsOpen(true)
    setChange(true)
  }, [fetchReportsData]);

  useEffect(() => {
    fetchReportsData();
  }, [fetchReportsData]);

  const getValues = (data) =>{
    
    const list = [];
    monitorPerformanceDataOriginal.forEach((a) =>{

        if(data === "semester"){
            if(!list.includes(a.semester)){
                list.push(a.semester);
            }
        }else if(data === "courses" && program !== ''){
            if(!list.includes(a.course) && a.program === program){
                list.push(a.course);
            }
        }else if(data === "professors"){
          if(!list.includes(a.professor)
            && (!semester || a.semester === semester)
            && (!program || a.program === program)
            && (!course || a.course === course)
          ){
                list.push(a.professor);

            }
        }else if(data === "programs"){
            if(!list.includes(a.program)){
                list.push(a.program);
            }
        }else {
          if(!list.includes(a.name)
            && (!semester || a.semester === semester)
            && (!program || a.program === program)
            && (!course || a.course === course)
            && (!professor || a.professor === professor)
          ){
                list.push(a.name);
            }
        }
       
    });
    return list;
  }

  // const monitorPerformanceDataOriginal = [
  //   { name: 'Monitor A', Completadas: 12, Tardias: 3, Pendientes: 2, semestre: '2024-1', programa: 'Ingenieria de Sistemas', curso: 'POO', profesor: 'Claudia' },
  // ];

  //  Función que aplica los filtros actuales
  const applyFilters = (data) => {
    if (!Array.isArray(data)) {
      console.warn("applyFilters recibió datos que no son un array:", data);
      return [];
    }

    return data.filter(d =>
      (!semester || d.semester === semester) &&
      (!program || d.program === program) &&
      (!course || d.course === course) &&
      (!professor || d.professor === professor) &&
      (!monitor || d.name === monitor)
    );
    
  };

  const applyAttendanceFilters = (data) => {
    if (!Array.isArray(data)) {
      console.warn("applyAttendanceFilters recibió datos que no son un array:", data);
      return [];
    }
    return data.filter(d => {
      if (!d) return false;

      const semesterMatch = !semester || d.semestre === semester;

      const courseMatch = !course ||
        (Array.isArray(d.asistencia_por_curso) && 
         d.asistencia_por_curso.some(item => item.curso === course));

      return semesterMatch && courseMatch;
    });
  };

  const filteredAttendanceData = useMemo(
    () => applyAttendanceFilters(asistenciaDataOriginal),
    [asistenciaDataOriginal, semester, course]
  );

  const dashboardMonitorData = useMemo(
    () => applyFilters(monitorPerformanceDataOriginal),
    [monitorPerformanceDataOriginal, semester, program, course, professor, monitor]
  );

  const dashboardProfessorData = useMemo(
    () => professorDataOriginal.filter(d =>
      (!semester || d.semester === semester) &&
      (!program || d.program === program) &&
      (!course || d.course === course) &&
      (!professor || d.name === professor)
    ),
    [professorDataOriginal, semester, program, course, professor]
  );

  const professorChartData = useMemo(() => {
    const groupedByProfessor = new Map();

    dashboardProfessorData.forEach((item) => {
      const professorName = normalizeProfessorName(item.name || 'Profesor');
      if (!groupedByProfessor.has(professorName)) {
        groupedByProfessor.set(professorName, {
          name: professorName,
          completed: 0,
          pending: 0,
          late: 0
        });
      }

      const current = groupedByProfessor.get(professorName);
      current.completed += Number(item.completed) || 0;
      current.pending += Number(item.pending) || 0;
      current.late += Number(item.late) || 0;
    });

    return Array.from(groupedByProfessor.values());
  }, [dashboardProfessorData]);

  const dashboardMetrics = useMemo(() => {
    const totalMonitors = new Set(dashboardMonitorData.map(d => d.name)).size;
    const totalProfessors = new Set(dashboardMonitorData.map(d => d.professor)).size;

    const monitorTotals = dashboardMonitorData.reduce((acc, item) => {
      acc.completed += item.completed || 0;
      acc.pending += item.pending || 0;
      acc.late += item.late || 0;
      return acc;
    }, { completed: 0, pending: 0, late: 0 });

    const professorTotals = dashboardProfessorData.reduce((acc, item) => {
      acc.completed += item.completed || 0;
      acc.pending += item.pending || 0;
      acc.late += item.late || 0;
      return acc;
    }, { completed: 0, pending: 0, late: 0 });

    const monitorBase = monitorTotals.completed + monitorTotals.pending + monitorTotals.late;
    const professorBase = professorTotals.completed + professorTotals.pending + professorTotals.late;

    return {
      totalMonitors,
      totalProfessors,
      monitorCompliance: monitorBase > 0 ? Math.round((monitorTotals.completed / monitorBase) * 100) : 0,
      professorCompliance: professorBase > 0 ? Math.round((professorTotals.completed / professorBase) * 100) : 0
    };
  }, [dashboardMonitorData, dashboardProfessorData]);

  const periodComparison = useMemo(() => {
    const parseSemesterOrder = (value) => {
      const normalized = String(value || '').trim();
      const match = normalized.match(/(\d{4})\D*(\d{1,2})?/);
      if (!match) return -1;

      const year = Number(match[1]) || 0;
      const term = Number(match[2]) || 0;
      return (year * 100) + term;
    };

    const scopedByFilters = monitorPerformanceDataOriginal.filter((item) =>
      (!program || item.program === program) &&
      (!course || item.course === course) &&
      (!professor || item.professor === professor) &&
      (!monitor || item.name === monitor)
    );

    const semesters = [...new Set(scopedByFilters.map((item) => item.semester).filter(Boolean))]
      .sort((a, b) => parseSemesterOrder(b) - parseSemesterOrder(a));

    if (semesters.length === 0) {
      return {
        currentSemester: null,
        previousSemester: null,
        currentCompliance: 0,
        previousCompliance: 0,
        delta: 0
      };
    }

    const currentSemester = semester || semesters[0];
    const currentIndex = semesters.findIndex((value) => value === currentSemester);
    const previousSemester = currentIndex >= 0 && currentIndex < semesters.length - 1
      ? semesters[currentIndex + 1]
      : null;

    const getSemesterBreakdown = (targetSemester) => {
      if (!targetSemester) {
        return {
          completed: 0,
          pending: 0,
          late: 0,
          compliance: 0
        };
      }

      const totals = scopedByFilters
        .filter((item) => item.semester === targetSemester)
        .reduce((acc, item) => {
          acc.completed += Number(item.completed) || 0;
          acc.pending += Number(item.pending) || 0;
          acc.late += Number(item.late) || 0;
          return acc;
        }, { completed: 0, pending: 0, late: 0 });

      const totalBase = totals.completed + totals.pending + totals.late;
      if (totalBase === 0) {
        return {
          completed: 0,
          pending: 0,
          late: 0,
          compliance: 0
        };
      }

      const completed = Math.round((totals.completed / totalBase) * 100);
      const pending = Math.round((totals.pending / totalBase) * 100);
      const late = Math.max(0, 100 - completed - pending);

      return {
        completed,
        pending,
        late,
        compliance: completed
      };
    };

    const currentBreakdown = getSemesterBreakdown(currentSemester);
    const previousBreakdown = getSemesterBreakdown(previousSemester);
    const currentCompliance = currentBreakdown.compliance;
    const previousCompliance = previousBreakdown.compliance;

    return {
      currentSemester,
      previousSemester,
      currentCompliance,
      previousCompliance,
      delta: currentCompliance - previousCompliance,
      currentBreakdown,
      previousBreakdown
    };
  }, [monitorPerformanceDataOriginal, semester, program, course, professor, monitor]);

  const periodComparisonChartData = useMemo(() => {
    if (!periodComparison.currentSemester) return [];

    const rows = [
      {
        periodo: `${periodComparison.currentSemester} (actual)`,
        cumplimiento: periodComparison.currentCompliance
      }
    ];

    if (periodComparison.previousSemester) {
      rows.push({
        periodo: `${periodComparison.previousSemester} (anterior)`,
        cumplimiento: periodComparison.previousCompliance
      });
    }

    const maxCompliance = Math.max(...rows.map((row) => row.cumplimiento));
    const minCompliance = Math.min(...rows.map((row) => row.cumplimiento));

    return rows.map((row) => ({
      ...row,
      barColor: row.cumplimiento === maxCompliance && maxCompliance !== minCompliance
        ? 'rgb(0, 196, 159)'
        : row.cumplimiento === minCompliance && maxCompliance !== minCompliance
          ? 'rgb(255, 82, 82)'
          : 'rgb(0, 196, 159)'
    }));

  }, [periodComparison]);

  const periodComparisonMessage = useMemo(() => {
    if (!periodComparison.currentSemester) {
      return 'Sin datos suficientes para comparar períodos.';
    }
    if (!periodComparison.previousSemester) {
      return 'No hay período anterior disponible para comparar.';
    }
    if (periodComparison.delta > 0) {
      return `El cumplimiento actual ha subido ${periodComparison.delta}% frente al período anterior.`;
    }
    if (periodComparison.delta < 0) {
      return `El cumplimiento actual ha bajado ${Math.abs(periodComparison.delta)}% frente al período anterior.`;
    }
    return 'El cumplimiento actual se mantuvo igual frente al período anterior.';
  }, [periodComparison]);

  const chartReadyAttendanceData = filteredAttendanceData.map(d => {
    let displayValue;
    let attendance;
    if (course) {
      const courseEntry = d.asistencia_por_curso?.find(item => item.curso === course);
      displayValue = courseEntry ? courseEntry.cantidad : 0;
      attendance = courseEntry? courseEntry.estudiantes : [];
    } else {
      displayValue = d.total_mes;
      attendance = [];
    }

    return {
      mes: d.mes,
      semestre: d.semestre, 
      asistencia: displayValue,
      asistentes:attendance
    };
  });

  const lineName = course ? `Asistentes - ${course}` : "Total Asistentes";

   const exportToCSV = (data, filename, filters) => {
    if (!data || data.length === 0) return;

    const csvRows = [];

    // Tabla principal
    const headers = Object.keys(data[0]);
    csvRows.push(headers.join(','));

    data.forEach(row => {
      const values = headers.map(header => `"${row[header] ?? ''}"`);
      csvRows.push(values.join(','));
    });

    // Separación visual
    csvRows.push('', '', 'Filtros aplicados:');

    // Filtros con etiquetas
    csvRows.push('Filtro,Valor');
    csvRows.push(`Semestre,${filters.semester}`);
    csvRows.push(`Programa,${filters.program}`);
    csvRows.push(`Curso,${filters.course}`);

    if (filters.professor && filters.professor.trim() !== '') {
      csvRows.push(`Profesor,${filters.professor}`);
    }

    if (filters.monitor && filters.monitor.trim() !== '') {
      csvRows.push(`Monitor,${filters.monitor}`);
    }

    // Crear blob y descargar
    const csvString = csvRows.join('\n');
    const blob = new Blob([csvString], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = `${filename}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const exportToPDF = (data, filename, filters) => {
    if (!data || data.length === 0) return;

    const normalizeValue = (value) => {
      if (value === null || value === undefined) return '';
      if (Array.isArray(value)) return value.join(' | ');
      if (typeof value === 'object') return JSON.stringify(value);
      return String(value);
    };

    const pdf = new jsPDF({ orientation: 'landscape', unit: 'pt', format: 'a4' });
    const headers = Object.keys(data[0]);
    const rows = data.map((row) => headers.map((header) => normalizeValue(row[header])));

    pdf.setFontSize(14);
    pdf.text(`Reporte: ${filename.replaceAll('_', ' ')}`, 40, 40);
    pdf.setFontSize(10);
    pdf.text(
      `Generado: ${new Date().toLocaleString('es-CO')} | Periodo: ${filters.semester || 'Todos'} | Departamento: ${filters.program || 'Todos'} | Profesor: ${filters.professor || 'Todos'} | Monitor: ${filters.monitor || 'Todos'}`,
      40,
      58
    );

    autoTable(pdf, {
      startY: 72,
      head: [headers],
      body: rows,
      styles: { fontSize: 8, cellPadding: 4 },
      headStyles: { fillColor: [84, 84, 232] }
    });

    pdf.save(`${filename}.pdf`);
  };
  //const categoryUsageData = applyFilters(categoryUsageDataOriginal);
  const asistenciaData = chartReadyAttendanceData;

  
  const semestersToShow = getValues("semester");
  const professorsToShow = getValues("professors");
  const programsToShow = getValues("programs");
  const monitorsToShow = getValues("monitors");


  const monitorPerformanceData = dashboardMonitorData;
  const professorData = professorChartData;
  const MONITOR_PAGE_SIZE = 10;

  const monitorSummaryRows = useMemo(() => {
    const groupedByMonitor = new Map();

    monitorPerformanceData.forEach((item) => {
      const monitorName = item.name || 'Sin nombre';
      if (!groupedByMonitor.has(monitorName)) {
        groupedByMonitor.set(monitorName, {
          name: monitorName,
          completed: 0,
          pending: 0,
          late: 0
        });
      }

      const current = groupedByMonitor.get(monitorName);
      current.completed += Number(item.completed) || 0;
      current.pending += Number(item.pending) || 0;
      current.late += Number(item.late) || 0;
    });

    return Array.from(groupedByMonitor.values()).map((item) => {
      const total = item.completed + item.pending + item.late;
      const compliance = total > 0 ? Math.round((item.completed / total) * 100) : 0;

      return {
        ...item,
        total,
        compliance
      };
    });
  }, [monitorPerformanceData]);

  const monitorRowOptions = useMemo(() => {
    return [...monitorSummaryRows]
      .map((item) => item.name)
      .sort((a, b) => a.localeCompare(b));
  }, [monitorSummaryRows]);

  const filteredSortedMonitorRows = useMemo(() => {
    const filtered = monitorSummaryRows.filter((item) =>
      !selectedMonitorRow || item.name === selectedMonitorRow
    );

    const sorted = [...filtered].sort((a, b) => {
      if (monitorSortBy === 'name_asc') return a.name.localeCompare(b.name);
      if (monitorSortBy === 'name_desc') return b.name.localeCompare(a.name);
      return a.name.localeCompare(b.name);
    });

    return sorted;
  }, [monitorSummaryRows, selectedMonitorRow, monitorSortBy]);

  const monitorPageCount = Math.max(1, Math.ceil(filteredSortedMonitorRows.length / MONITOR_PAGE_SIZE));

  const paginatedMonitorRows = useMemo(() => {
    const start = (monitorPage - 1) * MONITOR_PAGE_SIZE;
    return filteredSortedMonitorRows.slice(start, start + MONITOR_PAGE_SIZE);
  }, [filteredSortedMonitorRows, monitorPage]);

  const monitorPageStart = filteredSortedMonitorRows.length === 0 ? 0 : ((monitorPage - 1) * MONITOR_PAGE_SIZE) + 1;
  const monitorPageEnd = Math.min(monitorPage * MONITOR_PAGE_SIZE, filteredSortedMonitorRows.length);

  useEffect(() => {
    setMonitorPage(1);
  }, [selectedMonitorRow, monitorSortBy, monitorSummaryRows.length]);

  useEffect(() => {
    if (monitorPage > monitorPageCount) {
      setMonitorPage(monitorPageCount);
    }
  }, [monitorPage, monitorPageCount]);

  //Porcentaje actividades monitores


  useEffect(() => {
    if (monitorPerformanceData.length > 0) {
  
      // Calcula los porcentajes globales para monitores
      let totalCompleted = 0;
      let totalLate = 0;
      let totalPending = 0;
  
      monitorPerformanceData.forEach(item => {
        totalCompleted += item.completed || 0;
        totalLate += item.late || 0;
        totalPending += item.pending || 0;
      });
  
      const total = totalCompleted + totalLate + totalPending;
  
      if (total > 0) {
        setCompletedPercent(`${Math.round((totalCompleted / total) * 100)}%`);
        setPendingPercent(`${Math.round((totalPending / total) * 100)}%`);
        setLatePercent(`${Math.round((totalLate / total) * 100)}%`);
        setPorcentages([{
          completed: `${Math.round((totalCompleted / total) * 100)}%`,
          pending: `${Math.round((totalPending / total) * 100)}%`,
          late: `${Math.round((totalLate / total) * 100)}%`
        }]);
      }
    }
  }, [monitorPerformanceData]);
  
  //Porcentaje actividades profesores

useEffect(() => {
  if (professorData.length > 0) {
    let completed = 0;
    let late = 0;
    let pending = 0;
    
    professorData.forEach(item => {
      completed += item.completed || 0;
      late += item.late || 0;
      pending += item.pending || 0;
    });

    const total = completed + late + pending;

    if (total > 0) {
      const completedPercent = Math.round((completed / total) * 100);
      const latePercent = Math.round((late / total) * 100);
      const pendingPercent = Math.round((pending / total) * 100);

      setPorcentagesProfessor([{
        completed: `${completedPercent}%`,
        late: `${latePercent}%`,
        pending: `${pendingPercent}%`
      }]);

      setCompletedPercentProfessor(`${completedPercent}%`);
      setPendingPercentProfessor(`${pendingPercent}%`);
      setLatePercentProfessor(`${latePercent}%`);
    }
  }
}, [professorData]);


  const handleClose = () =>{
      setIsOpen(!isOpen)
      setChange(!change)
  }


  return (
    <div className="main">
      <PopUp
        show={isOpen}
        onClose={() => handleClose()}
      >{message}
      </PopUp>
      <div className="reports-top-bar">
        <h2 className="reports-title">Reportes de cumplimientos</h2>
        <div className="realtime-status">
          <span>{isRefreshing ? 'Actualizando datos...' : `Última actualización: ${lastUpdatedAt ? lastUpdatedAt.toLocaleTimeString('es-CO') : 'pendiente'}`}</span>
          <button
            type="button"
            className="chart-download-button"
            onClick={fetchReportsData}
            disabled={isRefreshing}
          >
            {isRefreshing ? 'Actualizando...' : 'Actualizar ahora'}
          </button>
        </div>
        <div className="filters-container">
          <div className="filter-group">
            <select value={semester} onChange={(e) => setSemester(e.target.value)}>
                  <option value="">Período</option>
                  {semestersToShow.map((semester, index) => (
                      <option key={index} value={semester}>
                      {semester}
                      </option>
                  ))}
            </select>
          </div>
          <div className="filter-group">
            <select value={program} onChange={(e) => setProgram(e.target.value)}>
              <option value="">Departamento</option>
                {programsToShow.map((program, index) => (
                    <option key={index} value={program}>
                    {program}
                    </option>
                ))}
            </select>
          </div>
          <div className="filter-group">
            <select value={professor} onChange={(e) => setProfessor(e.target.value)}>
            <option value="">Profesor</option>
                {professorsToShow.map((professor, index) => (
                    <option key={index} value={professor}>
                    {professor}
                    </option>
                ))}
            </select>
          </div>
          <div className="filter-group">
            <select value={monitor} onChange={(e) => setMonitor(e.target.value)}>
              <option value="">Monitor</option>
              {monitorsToShow.map((monitor, index) => (
                    <option key={index} value={monitor}>
                    {monitor}
                    </option>
                ))}
            </select>
          </div>
          <div className="filter-group">
            <button
              type="button"
              className="chart-download-button"
              onClick={() => {
                setSemester('');
                setProgram('');
                setCourse('');
                setProfessor('');
                setMonitor('');
              }}
            >
              Limpiar filtros
            </button>
          </div>
        </div>
      </div>
      <div className="reports-container">
        <VerticalNavbar />
        <div className="reports-content">
          <div className="dashboard-summary-card">
            <h3>Resumen general</h3>
            <div className="dashboard-summary-grid">
              <div className="dashboard-metric-card">
                <span>Profesores en reporte</span>
                <strong>{dashboardMetrics.totalProfessors}</strong>
              </div>
              <div className="dashboard-metric-card">
                <span>Monitores en reporte</span>
                <strong>{dashboardMetrics.totalMonitors}</strong>
              </div>
              <div className="dashboard-metric-card">
                <span>Cumplimiento de profesores</span>
                <strong>{dashboardMetrics.professorCompliance}%</strong>
              </div>
              <div className="dashboard-metric-card">
                <span>Cumplimiento de monitores</span>
                <strong>{dashboardMetrics.monitorCompliance}%</strong>
              </div>
              <div className="dashboard-metric-card">
                <span>Comparativo período actual vs anterior</span>
                <strong>
                  {periodComparison.previousSemester
                    ? `${periodComparison.delta >= 0 ? 'Ha subido' : 'Ha bajado'} ${Math.abs(periodComparison.delta)}%`
                    : 'N/D'}
                </strong>
                <span>
                  {periodComparison.currentSemester
                    ? `${periodComparison.currentSemester}: ${periodComparison.currentCompliance}%`
                    : 'Sin datos de período'}
                  {periodComparison.previousSemester
                    ? ` | ${periodComparison.previousSemester}: ${periodComparison.previousCompliance}%`
                    : ''}
                </span>
              </div>
            </div>
          </div>

          <div className="chart-card">
            <h3>Comparativo período actual vs anterior</h3>
            <BarChart width={500} height={260} data={periodComparisonChartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="periodo" />
              <YAxis allowDecimals={false} domain={[0, 100]} />
              <Tooltip formatter={(value) => [`${value}%`, 'Cumplimiento']} />
              <Legend />
              <Bar dataKey="cumplimiento" name="Cumplimiento">
                {periodComparisonChartData.map((entry) => (
                  <Cell key={`comparison-cell-${entry.periodo}`} fill={entry.barColor} />
                ))}
              </Bar>
            </BarChart>
            <p className={`comparison-message ${periodComparison.delta >= 0 ? 'positive' : 'negative'}`}>
              {periodComparisonMessage}
            </p>
          </div>
          
    <div className="chart-card">
      <h3>Rendimiento de monitores</h3>
      <BarChart width={500} height={260} data={paginatedMonitorRows}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" />
        <YAxis allowDecimals={false} />
        <Tooltip
          formatter={(value, name, props) => {
            const map = {
              completed: 'Completado',
              pending: 'Pendiente',
              late: 'Tarde'
            };

            const payload = props.payload;
            const completed = payload?.completed || 0;
            const pending = payload?.pending || 0;
            const late = payload?.late || 0;
            const total = completed + pending + late;
            const percentage = total > 0 ? Math.round((Number(value) / total) * 100) : 0;

            return [`${value} actividades (${percentage}%)`, map[name] || name];
          }}
        />
        <Legend
          formatter={(value) => {
            const map = {
              completed: 'Completado',
              pending: 'Pendiente',
              late: 'Tarde'
            };
            return map[value] || value;
          }}
        />
        <Bar dataKey="completed" stackId="a" fill="rgb(0, 196, 159)" />
        <Bar dataKey="pending" stackId="a" fill="rgb(255, 82, 82)" />
        <Bar dataKey="late" stackId="a" fill="rgb(255, 187, 40)" />
      </BarChart>

      <div className="monitor-table-controls">
        <label htmlFor="monitor-row-select">Selecciona monitor</label>
        <select
          id="monitor-row-select"
          value={selectedMonitorRow}
          onChange={(e) => setSelectedMonitorRow(e.target.value)}
        >
          <option value="">Todos los monitores</option>
          {monitorRowOptions.map((monitorName) => (
            <option key={monitorName} value={monitorName}>
              {monitorName}
            </option>
          ))}
        </select>
        <select value={monitorSortBy} onChange={(e) => setMonitorSortBy(e.target.value)}>
          <option value="name_asc">Ordenar: nombre (A-Z)</option>
          <option value="name_desc">Ordenar: nombre (Z-A)</option>
        </select>
      </div>

      <div className="monitor-table-wrapper">
        <table className="monitor-report-table">
          <thead>
            <tr>
              <th>Monitor</th>
              <th>Completadas</th>
              <th>Pendientes</th>
              <th>Tardías</th>
              <th>Total</th>
              <th>Cumplimiento</th>
            </tr>
          </thead>
          <tbody>
            {paginatedMonitorRows.length === 0 ? (
              <tr>
                <td colSpan={6}>No hay datos para los filtros actuales.</td>
              </tr>
            ) : (
              paginatedMonitorRows.map((item) => (
                <tr key={item.name}>
                  <td>{item.name}</td>
                  <td>{item.completed}</td>
                  <td>{item.pending}</td>
                  <td>{item.late}</td>
                  <td>{item.total}</td>
                  <td>{item.compliance}%</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="monitor-table-pagination">
        <span>{`Mostrando ${monitorPageStart}-${monitorPageEnd} de ${filteredSortedMonitorRows.length}`}</span>
        <div className="monitor-table-pagination-actions">
          <button
            className="chart-download-button"
            type="button"
            disabled={monitorPage <= 1}
            onClick={() => setMonitorPage((prev) => Math.max(1, prev - 1))}
          >
            Anterior
          </button>
          <span>{`Página ${monitorPage} de ${monitorPageCount}`}</span>
          <button
            className="chart-download-button"
            type="button"
            disabled={monitorPage >= monitorPageCount}
            onClick={() => setMonitorPage((prev) => Math.min(monitorPageCount, prev + 1))}
          >
            Siguiente
          </button>
        </div>
      </div>

      <div className="chart-download-container">
        <button className="chart-download-button" onClick={() => exportToCSV(monitorSummaryRows, 'Rendimiento_Monitores', {
              semester,
              program,
              course,
              professor,
              monitor
            })
          }>Descargar CSV</button>
        <button className="chart-download-button" onClick={() => exportToPDF(monitorSummaryRows, 'Rendimiento_Monitores', {
              semester,
              program,
              course,
              professor,
              monitor
            })
          }>Descargar PDF</button>
      </div>
    </div>


         {/* Porcentaje de tareas - MONITOR*/}
         <div className="chart-card">
            <h3>Tareas completadas, tardías y pendientes de monitores</h3>
            <div className="reports-summary">
              <div className="summary-card completadas">
                <h4>Completadas</h4>
                <p>{completedPercent}</p>
              </div>
              <div className="summary-card tardias">
                <h4>Completadas tardías</h4>
                <p>{latePercent}</p>
              </div>
              <div className="summary-card pendientes">
                <h4>Pendientes</h4>
                <p>{pendingPercent}</p>
              </div>
            </div>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(porcentages, 'Resumen_Tareas_Monitor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(porcentages, 'Resumen_Tareas_Monitor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
            </div>
          </div>

          {/* Asistencias */}
          <div className="chart-card">
            <h3> {`Asistencia a monitorías ${course ? `(${course})` : '(mensual)'}`}</h3>
            <LineChart width={500} height={300} data={asistenciaData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="mes" />
              <YAxis allowDecimals={false} />
              <Tooltip formatter={(value, name, props) => [`${value} asistentes`, lineName]} />
              <Legend />
              <Line
                type="monotone"
                dataKey="asistencia"
                stroke="#8884d8"
                name={lineName}
                activeDot={{ r: 8 }}
              />
            </LineChart>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(asistenciaData, `Asistencia_${lineName.replace(' ', '_')}`, {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(asistenciaData, `Asistencia_${lineName.replace(' ', '_')}`, {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
              {/* datos filtrados originales*/}
              {/* <button className="chart-download-button" onClick={() => exportToCSV(filteredAttendanceData, 'Asistencia_Detallada_Filtrada')}>Descargar Detalle Filtrado</button> */}
            </div>
          </div>

        {/* Gráfico de barras - PROFESOR */}
        <div className="chart-card">
          <h3>Rendimiento de profesores</h3>
            <BarChart width={500} height={300} data={professorData}>
              <CartesianGrid strokeDasharray="3 3" />
              
              {/* Mostrar solo el nombre (jeje sin el apellido) en el eje X */}
              <XAxis 
                dataKey="name" 
                tickFormatter={(value) => normalizeProfessorName(value)} 
              />
              
              <YAxis />

              <Tooltip 
                formatter={(value, name, props) => {
                  const map = {
                    completed: 'Completado',
                    pending: 'Pendiente',
                    late: 'Tarde',
                  };

                  const payload = props.payload;

                  if (payload && typeof value === 'number') {
                    const completed = payload.completed || 0;
                    const pending = payload.pending || 0;
                    const late = payload.late || 0;
                    const total = completed + pending + late;

                    const percentage = total > 0 ? Math.round((value / total) * 100) : 0;

                    return [`${value} actividades - ${percentage}%`, map[name] || name];
                  }

                  return [`${value}`, map[name] || name];
                }}

                labelFormatter={(label) => `Profesor: ${normalizeProfessorName(label)}`}
              />



              <Legend 
                formatter={(value) => {
                  const map = {
                    completed: 'Completado',
                    pending: 'Pendiente',
                    late: 'Tarde',
                  };
                  return map[value] || value;
                }} 
              />

              <Bar dataKey="completed" stackId="a" fill="rgb(0, 196, 159)" />
              <Bar dataKey="pending" stackId="a" fill="rgb(255, 82, 82)" />
              <Bar dataKey="late" stackId="a" fill="rgb(255, 187, 40)" />
            </BarChart>

          <div className="chart-download-container">
            <button className="chart-download-button" onClick={() => exportToCSV(professorData, 'Rendimiento_Profesores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
            <button className="chart-download-button" onClick={() => exportToPDF(professorData, 'Rendimiento_Profesores', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
          </div>
        </div>


         {/* Porcentaje de tareas - PROFESOR */}
         <div className="chart-card">
            <h3>Tareas completadas, tardías y pendientes de profesores</h3>
            <div className="reports-summary">
              <div className="summary-card completadas">
                <h4>Completadas</h4>
                <p>{completedPercentProfessor}</p>
              </div>
              <div className="summary-card tardias">
                <h4>Completadas tardías</h4>
                <p>{latePercentProfessor}</p>
              </div>
              <div className="summary-card pendientes">
                <h4>Pendientes</h4>
                <p>{pendingPercentProfessor}</p>
              </div>
            </div>
            <div className="chart-download-container">
              <button className="chart-download-button" onClick={() => exportToCSV(porcentagesProfessor, 'Resumen_Tareas_Profesor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar CSV</button>
              <button className="chart-download-button" onClick={() => exportToPDF(porcentagesProfessor, 'Resumen_Tareas_Profesor', {
                  semester,
                  program,
                  course,
                  professor,
                  monitor
                })
              }>Descargar PDF</button>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}

export default Reports;